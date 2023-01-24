package service.imp;

import model.Currency;
import service.CurrencyConversionService;

public class NbuCurrencyConversionService implements CurrencyConversionService {
    @Override
    public double getConversionRatio(Currency original, Currency target) {
        double originalRate= Double.parseDouble(original.getRate());
        double targetRate= Double.parseDouble(target.getRate());
        return originalRate/targetRate;
    }
}
