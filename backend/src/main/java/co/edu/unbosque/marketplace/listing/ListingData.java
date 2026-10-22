package co.edu.unbosque.marketplace.listing;

import java.math.BigDecimal;
import java.util.Set;

public record ListingData(String title, String description, String city, String location, String type, int capacity,
        BigDecimal pricePerNight, Set<String> amenityIds, String rules, Set<String> imageReferences) {
}
