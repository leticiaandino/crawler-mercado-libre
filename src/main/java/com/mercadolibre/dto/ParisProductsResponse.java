package com.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ParisProductsResponse {

    private int limit;
    private int count;
    private int total;
    private int offset;
    private List<Result> results;

    @Data
    public static class Result {
        private String key;
        private boolean published;
        private String brand;
        private Map<String, String> name;
        private MasterVariant masterVariant;
    }

    @Data
    public static class MasterVariant {
        private String sku;
        private Prices prices;
        private List<Image> images;
    }

    @Data
    public static class Prices {
        private Price regular;
        private Price offer;
    }

    @Data
    public static class Price {
        private Value value;
    }

    @Data
    public static class Value {
        private Long centAmount;
    }

    @Data
    public static class Image {
        private String url;
    }
}