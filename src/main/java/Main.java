import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import model.Currency;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import service.CurrencyConversionService;
import service.CurrencyModeService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class Main extends TelegramLongPollingBot {

    private final CurrencyModeService currencyModeService = CurrencyModeService.getInstance();

    private final CurrencyConversionService currencyConversionService = CurrencyConversionService.getInstance();
    URL url;

    ArrayList<Currency> currencies;

    {
        try {
            url = new URL("https://cbu.uz/oz/arkhiv-kursov-valyut/json/");
            URLConnection urlConnection = url.openConnection();
            Reader reader = new InputStreamReader(urlConnection.getInputStream());

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Type type = new TypeToken<ArrayList<Currency>>() {
            }.getType();
            currencies = gson.fromJson(reader, type);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "CentralBankConverterApi_bot";
    }

    @Override
    public String getBotToken() {
        return "5774107854:AAGAd9jDd9Z-0IMIxJLzgBXs21tHXltx5XU";
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallBack(update.getCallbackQuery());
        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            handleMessage(message);
        }
    }

    @SneakyThrows
    private void handleCallBack(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        String[] param = callbackQuery.getData().split(":");
        String action = param[0];
        Currency newCurrency = null;
        for (Currency currency : currencies) {
            if (currency.getCcyNmEN().equals(param[1])) {
                newCurrency = currency;
            }
        }
        switch (action) {
            case "ORIGINAL":
                currencyModeService.setOriginalCurrency(message.getChatId(), newCurrency);
                break;
            case "TARGET":
                currencyModeService.setTargetCurrency(message.getChatId(), newCurrency);
                break;
        }
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        Currency originalCurrency = currencyModeService.getOriginalCurrency(message.getChatId(), currencies);
        Currency targetCurrency = currencyModeService.getTargetCurrency(message.getChatId(), currencies);
        for (Currency currency : currencies) {
            buttons.add(
                    Arrays.asList(
                            InlineKeyboardButton.builder()
                                    .text(getCurrencyButton(originalCurrency, currency))
                                    .callbackData("ORIGINAL:" + currency.getCcyNmEN())
                                    .build(),
                            InlineKeyboardButton.builder()
                                    .text(getCurrencyButton(targetCurrency, currency))
                                    .callbackData("TARGET:" + currency.getCcyNmEN())
                                    .build()));
        }
        execute(EditMessageReplyMarkup.builder()
                .chatId(message.getChatId().toString())
                .messageId(message.getMessageId())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                .build());
    }

    @SneakyThrows
    private void handleMessage(Message message) {
        if (message.hasText() && message.hasEntities()) {
            Optional<MessageEntity> commandEntity =
                    message.getEntities().stream().filter(e -> "bot_command".equals(e.getType())).findFirst();
            if (commandEntity.isPresent()) {
                String command = message.getText()
                        .substring(commandEntity.get().getOffset(), commandEntity.get().getLength());
                switch (command) {
                    case "/set_currency":
                        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                        Currency originalCurrency = currencyModeService.getOriginalCurrency(message.getChatId(), currencies);
                        Currency targetCurrency = currencyModeService.getTargetCurrency(message.getChatId(), currencies);
                        for (Currency currency : currencies) {
                            buttons.add(
                                    Arrays.asList(
                                            InlineKeyboardButton.builder()
                                                    .text(getCurrencyButton(originalCurrency, currency))
                                                    .callbackData("ORIGINAL:" + currency.getCcyNmEN())
                                                    .build(),
                                            InlineKeyboardButton.builder()
                                                    .text(getCurrencyButton(targetCurrency, currency))
                                                    .callbackData("TARGET:" + currency.getCcyNmEN())
                                                    .build()));
                        }
                        execute(
                                SendMessage.builder()
                                        .text("Please choose Original and Target currencies")
                                        .chatId(message.getChatId().toString())
                                        .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                                        .build()
                        );
                        break;
                    case "/exchange_rate":
                        ArrayList<String> text=new ArrayList<>();
                        for (Currency currency : currencies) {
                            double dif= Double.parseDouble(currency.getDiff());
                            System.out.println(dif);
                            if (dif>0) {
                                text.add("1 " + getCurrencyName(currency) + " || " + currency.getCcy() + " || " + currency.getCode() +
                                        " || " + currency.getDiff() + " \uD83D\uDCC8|| " + currency.getRate() + "\n\n");
                            }else {
                                text.add("1 " + getCurrencyName(currency) + " || " + currency.getCcy() + " || " + currency.getCode() +
                                        " || " + currency.getDiff() + " \uD83D\uDCC9|| " + currency.getRate() + "\n\n");
                            }
                        }
                        execute(
                                SendMessage.builder()
                                        .text("Currency Exchange Rate for Today\n\n" +
                                                "Currency    || Ccy || Code || Differance || Rate \n\n"+ text)
                                        .chatId(message.getChatId().toString())
                                        .build()
                        );
                        break;
                }

            }
        }
        if (message.hasText()) {
            String messageText = message.getText();
            Optional<Double> value = parseDouble(messageText);
            Currency targetCurrency = currencyModeService.getTargetCurrency(message.getChatId(), currencies);
            Currency originalCurrency = currencyModeService.getOriginalCurrency(message.getChatId(), currencies);
            double conversionRatio = currencyConversionService.getConversionRatio(originalCurrency, targetCurrency);
            double originalCurrencyRatio= Double.parseDouble(originalCurrency.getRate());
            double targetCurrencyRatio= Double.parseDouble(targetCurrency.getRate());
            if (value.isPresent()) {
                execute(
                        SendMessage.builder()
                                .chatId(message.getChatId().toString())
                                .text(String.format("%4.2f %s is %4.2f %s " +
                                                "\n %4.2f %s is %4.2f UZB Soum\uD83C\uDDFA\uD83C\uDDFF" +
                                                "\n%s Rate: %4.2f",
                                        value.get(), getCurrencyName(originalCurrency), (value.get() * conversionRatio), getCurrencyName(targetCurrency),
                                        value.get(),getCurrencyName(originalCurrency),(value.get()*originalCurrencyRatio),
                                        getCurrencyName(originalCurrency),originalCurrencyRatio))
                                .build());
                return;
            }
        }
    }

    @SneakyThrows
    private Optional<Double> parseDouble(String messageText) {
        try {
            return Optional.of(Double.parseDouble(messageText));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SneakyThrows
    public static void main(String[] args) {
        Main main = new Main();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(main);
    }

    private String getCurrencyButton(Currency saved, Currency currant) {

        return saved == currant ? getCurrencyName(currant) + "✅" : getCurrencyName(currant);
    }

    private String getCurrencyName(Currency currency) {
        switch (currency.getCcyNmEN()) {
            case "US Dollar":
                return currency.getCcyNmEN() + "\uD83C\uDDFA\uD83C\uDDF8";
            case "Euro":
                return currency.getCcyNmEN() + "\uD83C\uDDEA\uD83C\uDDFA";
            case "Russian Ruble":
                return currency.getCcyNmEN() + "\uD83C\uDDF7\uD83C\uDDFA";
            case "Pound Sterling":
                return currency.getCcyNmEN() + "\uD83C\uDDEC\uD83C\uDDE7";
            case "Japan Yen":
                return currency.getCcyNmEN() + "\uD83C\uDDEF\uD83C\uDDF5";
            case "Azerbaijan Manat":
                return currency.getCcyNmEN() + "\uD83C\uDDE6\uD83C\uDDFF";
            case "Bangladesh Taka":
                return currency.getCcyNmEN() + "\uD83C\uDDE7\uD83C\uDDE9";
            case "Bulgarian Lev":
                return currency.getCcyNmEN() + "\uD83C\uDDE7\uD83C\uDDEC";
            case "Bahraini Dinar":
                return currency.getCcyNmEN() + "\uD83C\uDDE7\uD83C\uDDED";
            case "Brunei Dollar":
                return currency.getCcyNmEN() + "\uD83C\uDDE7\uD83C\uDDF3";
            case "Brazilian Real":
                return currency.getCcyNmEN() + "\uD83C\uDDE7\uD83C\uDDF7";
            case "Belarusian Ruble":
                return currency.getCcyNmEN() + "\uD83C\uDDE7\uD83C\uDDFE";
            case "Canadian Dollar":
                return currency.getCcyNmEN() + "\uD83C\uDDE8\uD83C\uDDE6";
            case "Swiss Franc":
                return currency.getCcyNmEN() + "\uD83C\uDDE8\uD83C\uDDED";
            case "Yuan Renminbi":
                return currency.getCcyNmEN() + "\uD83C\uDDE8\uD83C\uDDF3";
            case "Cuban Peso":
                return currency.getCcyNmEN() + "\uD83C\uDDE8\uD83C\uDDFA";
            case "Czech Koruna":
                return currency.getCcyNmEN() + "\uD83C\uDDE8\uD83C\uDDFF";
            case "Danish Krone":
                return currency.getCcyNmEN() + "\uD83C\uDDE9\uD83C\uDDF0";
            case "Algerian Dinar":
                return currency.getCcyNmEN() + "\uD83C\uDDE9\uD83C\uDDFF";
            case "Egyptian Pound":
                return currency.getCcyNmEN() + "\uD83C\uDDEA\uD83C\uDDEC";
            case "AF Afghani":
                return currency.getCcyNmEN() + "\uD83C\uDDE6\uD83C\uDDEB";
            case "Argentine Peso":
                return currency.getCcyNmEN() + "\uD83C\uDDE6\uD83C\uDDF7";
            case "Georgian Lari":
                return currency.getCcyNmEN() + "\uD83C\uDDEC\uD83C\uDDEA";
            case "Hong Kong Dollar":
                return currency.getCcyNmEN() + "\uD83C\uDDED\uD83C\uDDF0";
            case "Hungarian Forint":
                return currency.getCcyNmEN() + "\uD83C\uDDED\uD83C\uDDFA";
            case "Rupiah":
                return currency.getCcyNmEN() + "\uD83C\uDDEE\uD83C\uDDE9";
            case "New Israeli Sheqel":
                return currency.getCcyNmEN() + "\uD83C\uDDEE\uD83C\uDDF1";
            case "Indian Rupee":
                return currency.getCcyNmEN() + "\uD83C\uDDEE\uD83C\uDDF3";
            case "Iraqi Dinar":
                return currency.getCcyNmEN() + "\uD83C\uDDEE\uD83C\uDDF6";
            case "Iranian Rial":
                return currency.getCcyNmEN() + "\uD83C\uDDEE\uD83C\uDDF7";
            case "Iceland Krona":
                return currency.getCcyNmEN() + "\uD83C\uDDEE\uD83C\uDDF8";
            case "Jordanian Dinar":
                return currency.getCcyNmEN() + "\uD83C\uDDEF\uD83C\uDDF4";
            case "Australian Dollar":
                return currency.getCcyNmEN() + "\uD83C\uDDE6\uD83C\uDDFA";
            case "Kyrgyzstan Som":
                return currency.getCcyNmEN() + "\uD83C\uDDF0\uD83C\uDDEC";
            case "Riel":
                return currency.getCcyNmEN() + "\uD83C\uDDF0\uD83C\uDDED";
            case "The Korean Republic Won":
                return currency.getCcyNmEN() + "\uD83C\uDDF0\uD83C\uDDF7";
            case "Kuwaiti Dinar":
                return currency.getCcyNmEN() + "\uD83C\uDDF0\uD83C\uDDFC";
            case "Kazakhstan Tenge":
                return currency.getCcyNmEN() + "\uD83C\uDDF0\uD83C\uDDFF";
            case "Lao Kip":
                return currency.getCcyNmEN() + "\uD83C\uDDF1\uD83C\uDDE6";
            case "Lebanese Pound":
                return currency.getCcyNmEN() + "\uD83C\uDDF1\uD83C\uDDE7";
            case "Libyan Dinar":
                return currency.getCcyNmEN() + "\uD83C\uDDF1\uD83C\uDDFE";
            case "Moroccan Dirham":
                return currency.getCcyNmEN() + "\uD83C\uDDF2\uD83C\uDDE6";
            case "Moldovan Leu":
                return currency.getCcyNmEN() + "\uD83C\uDDF2\uD83C\uDDE9";
            case "Kyat":
                return currency.getCcyNmEN() + "\uD83C\uDDF2\uD83C\uDDF2";
            case "Tugrik":
                return currency.getCcyNmEN() + "\uD83C\uDDF2\uD83C\uDDF3";
            case "Mexican Peso":
                return currency.getCcyNmEN() + "\uD83C\uDDF2\uD83C\uDDFD";
            case "Malaysian Ringgit":
                return currency.getCcyNmEN() + "\uD83C\uDDF2\uD83C\uDDFE";
            case "Norwegian Krone":
                return currency.getCcyNmEN() + "\uD83C\uDDF3\uD83C\uDDF4";
            case "New Zealand Dollar":
                return currency.getCcyNmEN() + "\uD83C\uDDF3\uD83C\uDDFF";
            case "Rial Omani":
                return currency.getCcyNmEN() + "\uD83C\uDDF4\uD83C\uDDF2";
            case "Philippine Piso":
                return currency.getCcyNmEN() + "\uD83C\uDDF5\uD83C\uDDED";
            case "Pakistan Rupee":
                return currency.getCcyNmEN() + "\uD83C\uDDF5\uD83C\uDDF0";
            case "Polish Zloty":
                return currency.getCcyNmEN() + "\uD83C\uDDF5\uD83C\uDDF1";
            case "Qatari Rial":
                return currency.getCcyNmEN() + "\uD83C\uDDF6\uD83C\uDDE6";
            case "Romanian Leu":
                return currency.getCcyNmEN() + "\uD83C\uDDF7\uD83C\uDDF4";
            case "Serbian Dinar":
                return currency.getCcyNmEN() + "\uD83C\uDDF7\uD83C\uDDF8";
            case "Armenian Dram":
                return currency.getCcyNmEN() + "\uD83C\uDDE6\uD83C\uDDF2";
        }
        return currency.getCcyNmEN();
    }
}
