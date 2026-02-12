package wfg.ltv_econ.economy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import wfg.ltv_econ.economy.CommodityCell.PriceType;

public class CommodityPriceFuncTest {

    public static final float tolerance = 1e-3f;

    private final float price(PriceType type, int amount, double stored, float basePrice, float preferred) {
        return CommodityCell.getUnitPrice(type, amount, stored, basePrice, preferred);
    }

    @ParameterizedTest
    @CsvSource({
        "MARKET_BUYING, 0, 1000, 150, 1000, 150",
        "MARKET_BUYING, 1, 1000, 150, 1000, 149.9363",
        "MARKET_BUYING, 10, 1000, 150, 1000, 149.3664",
        "MARKET_BUYING, 100, 1000, 150, 1000, 143.99211",
        "MARKET_BUYING, 1000, 1000, 150, 1000, 109.56947",
        "MARKET_BUYING, 2000, 1000, 150, 1000, 89.57382",
        "MARKET_BUYING, 10000, 1000, 150, 1000, 43.287697",
        "MARKET_BUYING, 50000, 1000, 150, 1000, 16.07161",
        "MARKET_BUYING, 900000, 1000, 150, 1000, 15.0",
    })
    void buyInEqualibrium1(String typeStr, int amount, double stored, float basePrice, float preferred,
        float expected
    ) {
        final PriceType type = PriceType.valueOf(typeStr);
        final float result = price(type, amount, stored, basePrice, preferred);

        assertEquals(expected, result, tolerance);
    }

    @ParameterizedTest
    @CsvSource({
        "MARKET_SELLING, 0, 1000, 150, 1000, 150",
        "MARKET_SELLING, 1, 1000, 150, 1000, 150.08632",
        "MARKET_SELLING, 10, 1000, 150, 1000, 150.86873",
        "MARKET_SELLING, 100, 1000, 150, 1000, 159.29622",
        "MARKET_SELLING, 200, 1000, 150, 1000, 170.19005",
        "MARKET_SELLING, 500, 1000, 150, 1000, 219.13895",
        "MARKET_SELLING, 900, 1000, 150, 1000, 458.37503",
        "MARKET_SELLING, 999, 1000, 150, 1000, 600.0",
        "MARKET_SELLING, 1000, 1000, 150, 1000, 600.0",
        "MARKET_SELLING, 1001, 1000, 150, 1000, 600.0",
        "MARKET_SELLING, 1500, 1000, 150, 1000, 600.0",
        "MARKET_SELLING, 3000, 1000, 150, 1000, 600.0",
    })
    void sellInEqualibrium1(String typeStr, int amount, double stored, float basePrice, float preferred,
        float expected
    ) {
        final PriceType type = PriceType.valueOf(typeStr);
        final float result = price(type, amount, stored, basePrice, preferred);

        assertEquals(expected, result, tolerance);
    }

    @ParameterizedTest
    @CsvSource({
        "MARKET_BUYING, 0, 6000, 150, 1000, 32.708656",
        "MARKET_BUYING, 1, 6000, 150, 1000, 32.706337",
        "MARKET_BUYING, 10, 6000, 150, 1000, 32.685513",
        "MARKET_BUYING, 100, 6000, 150, 1000, 32.479324",
        "MARKET_BUYING, 1000, 6000, 150, 1000, 30.604836",
        "MARKET_BUYING, 2000, 6000, 150, 1000, 28.847013",
        "MARKET_BUYING, 10000, 6000, 150, 1000, 20.737034",
        "MARKET_BUYING, 50000, 6000, 150, 1000, 15.0",
        "MARKET_BUYING, 900000, 6000, 150, 1000, 15.0",
    })
    void buyInAbundance1(String typeStr, int amount, double stored, float basePrice, float preferred,
        float expected
    ) {
        final PriceType type = PriceType.valueOf(typeStr);
        final float result = price(type, amount, stored, basePrice, preferred);

        assertEquals(expected, result, tolerance);
    }

    @ParameterizedTest
    @CsvSource({
        "MARKET_SELLING, 0, 6000, 150, 1000, 19.108093",
        "MARKET_SELLING, 1, 6000, 150, 1000, 19.109924",
        "MARKET_SELLING, 10, 6000, 150, 1000, 19.126425",
        "MARKET_SELLING, 100, 6000, 150, 1000, 19.293427",
        "MARKET_SELLING, 1000, 6000, 150, 1000, 21.191355",
        "MARKET_SELLING, 2000, 6000, 150, 1000, 23.96436",
        "MARKET_SELLING, 5000, 6000, 150, 1000, 47.135265",
        "MARKET_SELLING, 5900, 6000, 150, 1000, 109.86676",
        "MARKET_SELLING, 6000, 6000, 150, 1000, 600.0",
        "MARKET_SELLING, 7500, 6000, 150, 1000, 600.0",
    })
    void sellInAbundance1(String typeStr, int amount, double stored, float basePrice, float preferred,
        float expected
    ) {
        final PriceType type = PriceType.valueOf(typeStr);
        final float result = price(type, amount, stored, basePrice, preferred);

        assertEquals(expected, result, tolerance);
    }

    @ParameterizedTest
    @CsvSource({
        "MARKET_BUYING, 0, 100, 150, 1000, 600.0",
        "MARKET_BUYING, 1, 100, 150, 1000, 600.0",
        "MARKET_BUYING, 10, 100, 150, 1000, 600.0",
        "MARKET_BUYING, 100, 100, 150, 1000, 600.0",
        "MARKET_BUYING, 1000, 100, 150, 1000, 306.45343",
        "MARKET_BUYING, 2000, 100, 150, 1000, 204.88693",
        "MARKET_BUYING, 10000, 100, 150, 1000, 70.670166",
        "MARKET_BUYING, 50000, 100, 150, 1000, 21.816486",
        "MARKET_BUYING, 900000, 100, 150, 1000, 15.0",
    })
    void buyInDeficit1(String typeStr, int amount, double stored, float basePrice, float preferred,
        float expected
    ) {
        final PriceType type = PriceType.valueOf(typeStr);
        final float result = price(type, amount, stored, basePrice, preferred);

        assertEquals(expected, result, tolerance);
    }

    @ParameterizedTest
    @CsvSource({
        "MARKET_SELLING, 0, 100, 150, 1000, 600.0",
        "MARKET_SELLING, 1, 100, 150, 1000, 600.0",
        "MARKET_SELLING, 10, 100, 150, 1000, 600.0",
        "MARKET_SELLING, 50, 100, 150, 1000, 600.0",
        "MARKET_SELLING, 90, 100, 150, 1000, 600.0",
        "MARKET_SELLING, 99, 100, 150, 1000, 600.0",
        "MARKET_SELLING, 100, 100, 150, 1000, 600.0",
        "MARKET_SELLING, 150, 100, 150, 1000, 600.0",
        "MARKET_SELLING, 500, 100, 150, 1000, 600.0",
    })
    void sellInDeficit1(String typeStr, int amount, double stored, float basePrice, float preferred,
        float expected
    ) {
        final PriceType type = PriceType.valueOf(typeStr);
        final float result = price(type, amount, stored, basePrice, preferred);

        assertEquals(expected, result, tolerance);
    }

    @ParameterizedTest
    @CsvSource({
        "MARKET_BUYING, 0, 0, 150, 0, 150.0",
        "MARKET_BUYING, 1, 0, 150, 0, 150.0",
        "MARKET_BUYING, 5, 0, 150, 0, 150.0",
        "MARKET_BUYING, 10, 0, 150, 0, 150.0",
        "MARKET_BUYING, 50, 0, 150, 0, 150.0",
    })
    void buyWithZeroLocalDemand(String typeStr, int amount, double stored, float basePrice,
        float preferred, float expected
    ) {
        final PriceType type = PriceType.valueOf(typeStr);
        final float result = price(type, amount, stored, basePrice, preferred);

        assertEquals(expected, result, tolerance);
    }

    @ParameterizedTest
    @CsvSource({
        "MARKET_SELLING, 0, 0, 150, 0, 150.0",
        "MARKET_SELLING, 1, 0, 150, 0, 150.0",
        "MARKET_SELLING, 5, 0, 150, 0, 150.0",
        "MARKET_SELLING, 10, 0, 150, 0, 150.0",
        "MARKET_SELLING, 50, 0, 150, 0, 150.0",
    })
    void sellWithZeroLocalDemand(String typeStr, int amount, double stored, float basePrice,
        float preferred, float expected
    ) {
        final PriceType type = PriceType.valueOf(typeStr);
        final float result = price(type, amount, stored, basePrice, preferred);

        assertEquals(expected, result, tolerance);
    }
}