package hu.gerab.payment.service;

import hu.gerab.payment.domain.Currency;
import java.math.BigDecimal;

public interface PaymentService {

  void processTransfer(
      String requestId, Long senderId, Long receiverId, BigDecimal amount, Currency currency);
}
