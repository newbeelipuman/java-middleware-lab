package study.middleware.mqreliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.messaging.support.MessageBuilder;
import study.middleware.mqreliability.shift.ShiftChangeRepository;
import study.middleware.mqreliability.transaction.ShiftTransactionListener;
import study.middleware.mqreliability.transaction.TransactionFactRepository;

class TransactionListenerTest {
    @Test
    void checkUsesDatabaseFactAndReturnsUnknownWhenDatabaseIsUnavailable() {
        TransactionFactRepository facts = Mockito.mock(TransactionFactRepository.class);
        ShiftTransactionListener listener = new ShiftTransactionListener(
                Mockito.mock(ShiftChangeRepository.class), facts, new ObjectMapper());
        var message = MessageBuilder.withPayload("{\"eventId\":\"evt-1\"}").build();

        when(facts.state("evt-1")).thenReturn(Optional.of("COMMITTED"));
        assertThat(listener.checkLocalTransaction(message))
                .isEqualTo(RocketMQLocalTransactionState.COMMIT);

        when(facts.state("evt-1")).thenThrow(new TransientDataAccessResourceException("down"));
        assertThat(listener.checkLocalTransaction(message))
                .isEqualTo(RocketMQLocalTransactionState.UNKNOWN);
    }
}
