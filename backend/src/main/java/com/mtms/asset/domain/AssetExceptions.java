package com.mtms.asset.domain;

public final class AssetExceptions {

    private AssetExceptions() {}

    public static class AssetNotFoundException extends RuntimeException {
        public AssetNotFoundException(String message) {
            super(message);
        }
    }

    public static class AssetCodeConflictException extends RuntimeException {
        public AssetCodeConflictException(String message) {
            super(message);
        }
    }

    public static class InvalidAssetCostException extends RuntimeException {
        public InvalidAssetCostException(String message) {
            super(message);
        }
    }

    public static class InvalidAssetDataException extends RuntimeException {
        public InvalidAssetDataException(String message) {
            super(message);
        }
    }
}
