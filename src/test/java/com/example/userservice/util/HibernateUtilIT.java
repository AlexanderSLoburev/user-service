package com.example.userservice.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.example.userservice.PostgresExtension;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;

@Tag("integration")
@ExtendWith(PostgresExtension.class)
class HibernateUtilIT {

  @AfterEach
  void tearDown() {
    HibernateUtil.shutdown();
  }

  @Test
  void when_getSessionFactoryIsCalled_then_factoryIsInitializedAndOpen() {
    SessionFactory factory = HibernateUtil.getSessionFactory();
    assertNotNull(factory);
    assertTrue(factory.isOpen());
  }

  @Test
  void when_getSessionFactoryIsCalledTwice_then_theSameInstanceIsReturned() {
    assertSame(HibernateUtil.getSessionFactory(),
               HibernateUtil.getSessionFactory());
  }

  @Test
  void when_shutdownIsCalled_then_factoryClosesAndNextCallRebuildsIt() {
    SessionFactory first = HibernateUtil.getSessionFactory();
    HibernateUtil.shutdown();

    assertFalse(first.isOpen());

    SessionFactory second = HibernateUtil.getSessionFactory();
    assertTrue(second.isOpen());
    assertNotSame(first, second);
  }

  @Test
  void when_shutdownIsCalledTwiceInARow_then_theSecondCallIsSafe() {
    HibernateUtil.getSessionFactory();
    HibernateUtil.shutdown();
    HibernateUtil.shutdown();
  }

  @RepeatedTest(value = 3,
                name = "round {currentRepetition}/{totalRepetitions}")
  @Timeout(20)
  void
  when_twoThreadsRequestTheFactoryConcurrently_then_bothGetTheSameInstance()
      throws Exception {

    HibernateUtil.shutdown(); // Precondition: there is no factory.

    CyclicBarrier startGate = new CyclicBarrier(2);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<SessionFactory> first = pool.submit(() -> race(startGate));
      Future<SessionFactory> second = pool.submit(() -> race(startGate));

      assertSame(first.get(15, TimeUnit.SECONDS),
                 second.get(15, TimeUnit.SECONDS));
    } finally {
      pool.shutdownNow();
    }
  }

  private static SessionFactory race(CyclicBarrier startGate) throws Exception {
    startGate.await(10, TimeUnit.SECONDS);
    return HibernateUtil.getSessionFactory();
  }
}