package service;

import model.Currency;
import service.imp.HashMapCurrencyModeService;

import java.util.ArrayList;

public interface CurrencyModeService {
    static CurrencyModeService getInstance(){return new HashMapCurrencyModeService(); }

    Currency getOriginalCurrency(long chatId, ArrayList<Currency> currencies);

    Currency getTargetCurrency(long chatId, ArrayList<Currency> currencies);

    void setOriginalCurrency(long chatId, Currency currency);

    void setTargetCurrency(long chatId, Currency currency);
}
