package service.imp;

import model.Currency;
import service.CurrencyModeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HashMapCurrencyModeService implements CurrencyModeService {
    private final Map<Long,Currency> originalCurrency=new HashMap<>();

    private final Map<Long,Currency> targetCurrency=new HashMap<>();
    public HashMapCurrencyModeService() {
        System.out.println("HASHMAP MODE is created");
    }

    @Override
    public Currency getOriginalCurrency(long chatId, ArrayList<Currency> currencies) {
        return originalCurrency.getOrDefault(chatId, currencies.get(0));
    }

    @Override
    public Currency getTargetCurrency(long chatId, ArrayList<Currency> currencies) {
        return targetCurrency.getOrDefault(chatId, currencies.get(0));
    }

    @Override
    public void setOriginalCurrency(long chatId, Currency currency) {
        originalCurrency.put(chatId, currency);
    }

    @Override
    public void setTargetCurrency(long chatId, Currency currency) {
        targetCurrency.put(chatId, currency);
    }
}
