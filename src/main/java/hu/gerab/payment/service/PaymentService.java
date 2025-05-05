package hu.gerab.payment.service;

import hu.gerab.payment.domain.Currency;
import java.math.BigDecimal;
import java.util.concurrent.Future;

public interface PaymentService {

  Future<Boolean> processTransaction(
      String requestId, long userId, BigDecimal amount, Currency currency);
}
