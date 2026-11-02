package com.anhanguera.library.service;

import com.anhanguera.library.IntegrationTestSupport;
import com.anhanguera.library.dtos.LendingDTO.LendingRequest;
import com.anhanguera.library.exception.BusinessRuleException;
import com.anhanguera.library.repository.LendRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

class LendingServiceConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private LendingService lendingService;

    @Autowired
    private LendRepository lendRepository;

    /**
     * Dois pedidos simultâneos de empréstimo do mesmo livro: só um pode ser registrado.
     * <p>
     * Roda fora da transação do teste, porque cada pedido precisa da própria transação
     * (como em duas requisições HTTP reais). Os empréstimos criados são apagados no final
     * para não afetar os outros testes, que compartilham o mesmo banco.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentLendingsOfTheSameBookRegisterOnlyOne() throws Exception {
        long isabelaId = idOf("/users", "name", "Isabela Rocha");
        long joaoId = idOf("/users", "name", "João Pereira");
        LocalDate today = today();
        List<Long> createdIds = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (String title : List.of("Vidas Secas", "São Bernardo", "Eu, Robô", "Frankenstein", "Fundação")) {
                long bookId = idOf("/books", "title", title);
                CyclicBarrier barrier = new CyclicBarrier(2);
                List<Future<Long>> results = new ArrayList<>();
                for (long userId : new long[]{isabelaId, joaoId}) {
                    results.add(executor.submit(() -> {
                        barrier.await(10, SECONDS);
                        LendingRequest request = new LendingRequest(userId, bookId, today, today.plusDays(14));
                        return lendingService.createLending(request).id();
                    }));
                }

                int registered = 0;
                for (Future<Long> result : results) {
                    try {
                        createdIds.add(result.get(30, SECONDS));
                        registered++;
                    } catch (ExecutionException e) {
                        assertThat(e.getCause())
                                .isInstanceOf(BusinessRuleException.class)
                                .hasMessage("Este livro já está emprestado.");
                    }
                }
                assertThat(registered).as("empréstimos registrados para \"%s\"", title).isEqualTo(1);
            }
        } finally {
            executor.shutdownNow();
            lendRepository.deleteAllById(createdIds);
        }
    }
}
