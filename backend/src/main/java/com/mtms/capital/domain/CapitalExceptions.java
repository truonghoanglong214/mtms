package com.mtms.capital.domain;

import java.net.URI;
import java.util.UUID;

public final class CapitalExceptions {
    private CapitalExceptions() {}

    public static class InvestorNotFoundException extends RuntimeException {
        private final UUID investorId;

        public InvestorNotFoundException(UUID investorId) {
            super("Investor not found: " + investorId);
            this.investorId = investorId;
        }

        public UUID getInvestorId() {
            return investorId;
        }

        public URI getTypeUri() {
            return URI.create("urn:mtms:error:investor-not-found");
        }
    }

    public static class AgreementNotFoundException extends RuntimeException {
        private final UUID agreementId;

        public AgreementNotFoundException(UUID agreementId) {
            super("Investor agreement not found: " + agreementId);
            this.agreementId = agreementId;
        }

        public UUID getAgreementId() {
            return agreementId;
        }

        public URI getTypeUri() {
            return URI.create("urn:mtms:error:agreement-not-found");
        }
    }

    public static class CommitmentNotFoundException extends RuntimeException {
        private final UUID commitmentId;

        public CommitmentNotFoundException(UUID commitmentId) {
            super("Capital commitment not found: " + commitmentId);
            this.commitmentId = commitmentId;
        }

        public UUID getCommitmentId() {
            return commitmentId;
        }

        public URI getTypeUri() {
            return URI.create("urn:mtms:error:commitment-not-found");
        }
    }

    public static class AgreementConflictException extends RuntimeException {
        private final String agreementRef;

        public AgreementConflictException(String agreementRef) {
            super("Agreement reference already exists in branch: " + agreementRef);
            this.agreementRef = agreementRef;
        }

        public String getAgreementRef() {
            return agreementRef;
        }

        public URI getTypeUri() {
            return URI.create("urn:mtms:error:agreement-conflict");
        }
    }

    public static class InvestorConflictException extends RuntimeException {
        private final String displayName;

        public InvestorConflictException(String displayName) {
            super("Investor with display name already exists in entity: " + displayName);
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public URI getTypeUri() {
            return URI.create("urn:mtms:error:investor-conflict");
        }
    }

    public static class InvalidContributionException extends RuntimeException {
        public InvalidContributionException(String message) {
            super(message);
        }

        public URI getTypeUri() {
            return URI.create("urn:mtms:error:invalid-contribution");
        }
    }

    public static class InvalidProfitTermException extends RuntimeException {
        public InvalidProfitTermException(String message) {
            super(message);
        }

        public URI getTypeUri() {
            return URI.create("urn:mtms:error:invalid-profit-term");
        }
    }
}
