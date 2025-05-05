package hu.gerab.payment.domain;

import lombok.Getter;

/** The accepted currencies of the system */
public enum Currency {
  USD;

  @Getter private String humanFriendlyName;

  Currency() {
    this.humanFriendlyName = this.name();
  }

  private Currency(String humanFriendlyName) {
    this.humanFriendlyName = humanFriendlyName;
  }
}
