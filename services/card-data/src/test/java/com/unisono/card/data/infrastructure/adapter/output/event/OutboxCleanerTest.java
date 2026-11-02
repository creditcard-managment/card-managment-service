package com.unisono.card.data.infrastructure.adapter.output.event;

import com.unisono.card.data.infrastructure.adapter.output.persistence.repository.JpaOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxCleanerTest {

    @Mock
    private JpaOutboxEventRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldDeletePublishedEventsOlderThanRetention() {
        OutboxCleaner cleaner = new OutboxCleaner(repository, transactionManager);
        ReflectionTestUtils.setField(cleaner, "retentionDays", 7);
        when(repository.deletePublishedBefore(any())).thenReturn(3);

        cleaner.cleanup();

        ArgumentCaptor<LocalDateTime> before = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deletePublishedBefore(before.capture());
        assertThat(before.getValue()).isCloseTo(LocalDateTime.now().minusDays(7), within(5, java.time.temporal.ChronoUnit.SECONDS));
        verify(repository).countByFailedAtIsNotNull();
    }
}
