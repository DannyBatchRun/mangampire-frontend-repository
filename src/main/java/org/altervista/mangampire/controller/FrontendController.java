package org.altervista.mangampire.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.altervista.mangampire.model.Client;
import org.altervista.mangampire.model.*;
import org.altervista.mangampire.model.SearchManga;
import org.altervista.mangampire.dto.ClientCart;
import org.altervista.mangampire.dto.EndpointRequest;
import org.altervista.mangampire.dto.RequestLogin;
import org.altervista.mangampire.dto.ShoppingCart;
import org.altervista.mangampire.service.FrontendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class FrontendController {
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private EndpointRequest backendService;
    @Autowired
    private FrontendService service;
    @Autowired
    private RequestLogin loginCached;
    @Autowired
    private ClientCart clientCartCached;
    @GetMapping("/")
    public String getLogin(Model model) throws JsonProcessingException {
        String page = service.getDashboardOrThePage(service.getSessionLogged(),"index");
        if(!page.equals("index")) {
            tryToLoginPlatform(loginCached, model);
        }
        return page;
    }
    @PatchMapping("/backend/url")
    public ResponseEntity<String> setBackendEndpoint(@Validated @RequestBody EndpointRequest request) {
        backendService.setEndpoint("http://" + request.getEndpoint());
        return ResponseEntity.ok().body("Endpoint of Backend Setted");
    }

    @GetMapping("/register")
    public String getRegistrationForm(Model model) throws JsonProcessingException {
        String page = (!service.getSessionLogged()) ? "registrati" : "dashboard";
        if(page.equals("dashboard")) {
            tryToLoginPlatform(loginCached, model);
        }
        return page;
    }
    @PostMapping("/login")
    public String tryToLoginPlatform(@ModelAttribute RequestLogin requestLogin, Model model) throws JsonProcessingException {
        if(requestLogin.getEmail() == null || requestLogin.getPassword() == null) {
            String backToPage = (service.getSessionLogged()) ? "dashboard" : "index";
            return service.getDashboardOrThePage(service.getSessionLogged(),backToPage);
        }
        String page = service.doLoginRequest(backendService,requestLogin);
        if(page.equalsIgnoreCase("dashboard")) {
            Client clientFound = service.getClientfromBackendService(backendService, requestLogin);
            model.addAttribute("idClient", clientFound.getIdClient());
            model.addAttribute("email", clientFound.getEmail());
            model.addAttribute("name", clientFound.getName());
            model.addAttribute("surname", clientFound.getSurname());
            model.addAttribute("dateBirth", clientFound.getDateBirth());
            model.addAttribute("cardQuantity", clientFound.getCardQuantity());
            String shoppingCart = service.getCartClientFromBackend(backendService, clientFound);
            String cardsJson = service.getCardsOfClientFromBackend(backendService, clientFound.getIdClient());
            if (shoppingCart == null) {
                model.addAttribute("totalPrice", "0");
                model.addAttribute("shoppingCart", "Carrello Vuoto");
            } else {
                List<Map<String, Object>> list = mapper.readValue(shoppingCart, new TypeReference<List<Map<String, Object>>>() {
                });
                double totalPrice = 0;
                StringBuilder formattedJson = new StringBuilder();
                for (Map<String, Object> map : list) {
                    String name = (String) map.get("name");
                    int volume = (Integer) map.get("volume");
                    int quantity = (Integer) map.get("quantity");
                    double price = (Double) map.get("price");
                    formattedJson.append("Nome: ").append(name).append(", Volume: ").append(volume).append(", Quantità: ").append(quantity).append("<br />");
                    totalPrice += price * quantity;
                    model.addAttribute("shoppingCart", formattedJson.toString());
                    model.addAttribute("totalPrice", String.format("%.2f", totalPrice));
                    clientCartCached.setCartFormatted(formattedJson.toString());
                    clientCartCached.setTotalPrice(totalPrice);
                }
            }
            List<Card> cardList = mapper.readValue(cardsJson, new TypeReference<List<Card>>() {});
            StringBuilder formattedCards = new StringBuilder();
            for (Card card : cardList) {
                String cardNumber = card.getCardNumber();
                String censoredCardNumber = "";
                censoredCardNumber = "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
                formattedCards.append("Numero Carta: ").append(censoredCardNumber).append("<br />");
            }
            if(formattedCards.toString().isEmpty()) {
                model.addAttribute("cards", "Nessuna Carta");
            } else {
                model.addAttribute("cards", formattedCards.toString());
            }
        }
        loginCached.setEmail(requestLogin.getEmail());
        loginCached.setPassword(requestLogin.getPassword());
        return page;
    }
    @GetMapping("/logout")
    public String doLogoutFromThePlatform () {
        if(service.getSessionLogged()) {
            service.setSessionLogged(false);
            loginCached.reset();
        }
        return "redirect:/";
    }

    @GetMapping("/card/register")
    public String getPageOfCardRegister(Model model) {
        String page = "";
        page = (service.getSessionLogged()) ? "addcard" : "index";
        model.addAttribute("email",loginCached.getEmail());
        return page;
    }
    @PostMapping("/return")
    public String returnToDashboard (Model model) throws JsonProcessingException {
        String page = "";
        if(service.getSessionLogged()) {
            page = tryToLoginPlatform(loginCached, model);
        } else {
            page = "index";
        }
        return page;
    }

    @GetMapping("/shop")
    public String goToTheShop (Model model) {
        String page = "";
        page = (service.getSessionLogged()) ? "negozio" : "index";
        model.addAttribute("email",loginCached.getEmail());
        return page;
    }

    @PostMapping("/cart/add")
    public String addMangaToCart(@ModelAttribute SearchManga manga, Model model) {
        String page = "";
        if(service.getSessionLogged()) {
            model.addAttribute("email", loginCached.getEmail());
            ShoppingCart shoppingCart = service.setShoppingCart(backendService, loginCached, manga);
            page = service.setAdditionCartRequestInBackend(backendService,shoppingCart);
        } else if (!service.getSessionLogged()) {
            page = "index";
        }
        return page;
    }
    @PostMapping("/card/submit")
    public String requestAddCardToBackEnd(@ModelAttribute Card card, Model model) {
        return service.addCardfromBackend(backendService,loginCached,card);
    }
    @GetMapping("/transaction/get")
    public String getThePageOfTransaction(Model model) throws JsonProcessingException {
        if(service.getSessionLogged()) {
            model.addAttribute("email", loginCached.getEmail());
            model.addAttribute("totalPrice", clientCartCached.getTotalPrice());
            Client clientFound = service.getClientfromBackendService(backendService, loginCached);
            String cardsJson = service.getCardsOfClientFromBackend(backendService, clientFound.getIdClient());
            List<Card> cardList = mapper.readValue(cardsJson, new TypeReference<List<Card>>() {});
            Set<String> formattedCardsAbsolute = new TreeSet<>();
            Set<String> formattedCardsComplete = new TreeSet<>();
            for (Card card : cardList) {
                formattedCardsAbsolute.add(card.getCardNumber());
                formattedCardsComplete.add(card.getCardNumber() + " - " + card.getCardHolderName() + " " + card.getCardHolderSurname());
            }
            model.addAttribute("cards", formattedCardsAbsolute);
            model.addAttribute("cardsComplete", formattedCardsComplete);
            return service.getPageDependingCartClient(clientCartCached);
        } else {
            return "index";
        }
    }
    @PostMapping("/transaction/submit")
    public String completeTransactionWithFrontend(@ModelAttribute("cardSelection") String cardNumber, @ModelAttribute Shipment shipment, Model model) {
        String page = "index";
        String message = "none";
        if(service.getSessionLogged()) {
            model.addAttribute("email", loginCached.getEmail());
            String responseBuy = service.askBackendToCompleteTransaction(backendService,loginCached,cardNumber);
            if(responseBuy.contains("is out of stock at the moment")) {
                page = "paymentfailed";
                message = "Uno o più manga risultano non disponibili. Per favore, riprova tra qualche giorno.";
                model.addAttribute("shipment", "Torna alla Dashboard");
            } else if (responseBuy.contains("has no enough credit for buy total cart")) {
                page = "paymentfailed";
                message = "La Carta non ha un importo sufficiente per continuare. Per favore, seleziona un altra carta.";
                model.addAttribute("shipment", "Torna alla Dashboard");
            } else if (responseBuy.contains("Shopping cart buyed successful")) {
                page = "paymentsuccess";
                message = "Acquisto Effettuato con Successo! I Manga verranno spediti ai seguenti dati :\n";
                String formattedShipment = "<h3>" + shipment.getName() + " " + shipment.getSurname() + "</h3>" + shipment.getAddress() + "<br />" + shipment.getPostalCode() + " " + shipment.getCity();
                model.addAttribute("shipment", formattedShipment);
            }
            model.addAttribute("message", message);
        } else {
            return "index";
        }
        return page;
    }
    @PostMapping("/register/submit")
    public String addClientToDatabase(@ModelAttribute Client client, Model model) throws JsonProcessingException {
        String page = "dashboard";
        if(!service.getSessionLogged()) {
            String message = "No message";
            Boolean isClientAdded = service.askBackendToAddClient(backendService, client);
            if (Boolean.TRUE.equals(isClientAdded)) {
                message = "Registrazione avvenuta! NON CONDIVIDERE LA PASSWORD CON NESSUNO! Puoi effettuare il login con i seguenti dati :<br />";
                String formattedRegistration = "<h3>" + client.getEmail() + "</h3>" + "Password : " + client.getPassword() + "<br />";
                page = "registrasuccess";
                model.addAttribute("registration", formattedRegistration);
            } else {
                message = "Qualcosa è andato storto, probabilmente l'indirizzo e-mail risulta già registrato. Riprova.<br />";
                model.addAttribute("registration", "Registrazione non riuscita.");
                page = "registrafailed";
            }
            model.addAttribute("message", message);
        } else {
            tryToLoginPlatform(loginCached, model);
        }
        return page;
    }
}
