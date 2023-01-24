package service;

import model.Currency;
import service.imp.NbuCurrencyConversionService;

public interface CurrencyConversionService {

    static CurrencyConversionService getInstance() {
        return new NbuCurrencyConversionService();
    }

    double getConversionRatio(Currency original, Currency target);

}
