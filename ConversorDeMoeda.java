
package ConversorDeMoeda;

import com.google.gson.Gson;
import data.TaxasCambio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class ConversorDeMoeda {

    private static final String API_BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static String apiKey;

    private static void loadApiKey() {
        Properties prop = new Properties();
        try (InputStream input = ConversorDeMoeda.class.getClassLoader().getResourceAsStream("config.properties"))
        {
            if (input == null) {
                System.out.println("Erro: Arquivo 'config.properties' não encontrado na raiz do projeto.");
                apiKey = null;
                return;
            }

            prop.load(input);
            apiKey = prop.getProperty("apiKey");

            if (apiKey == null || apiKey.trim().isEmpty())
            {
                System.out.println("Erro: 'apiKey' não encontrada ou está vazia no arquivo config.properties.");
                apiKey = null;
            }

        } catch (IOException ex) {
            System.out.println("Erro ao ler o arquivo config.properties: " + ex.getMessage());
            ex.printStackTrace();
            apiKey = null;
        }
    }

    public static TaxasCambio obterTaxas(String moedaBase) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("Chave de API não disponível para fazer a requisição.");
            return null;
        }

        String url = API_BASE_URL + apiKey + "/latest/" + moedaBase;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            HttpResponse<String> response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                if (response.body().contains("\"result\":\"success\"")) {
                    Gson gson = new Gson();
                    TaxasCambio taxas = gson.fromJson(response.body(), TaxasCambio.class);

                    if (taxas != null && taxas.getConversion_rates() == null) {
                        System.out.println("Aviso: 'conversion_rates' veio nulo na resposta da API para " + moedaBase);
                    }

                    return taxas;
                } else {
                    System.out.println("Erro: resposta da API (JSON não indica sucesso): " + response.body());
                    return null;
                }
            } else {
                System.out.println("Erro: requisição HTTP: Código " + response.statusCode());
                return null;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Erro: ao obter taxas câmbio: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        loadApiKey();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("Erro ao carregar a chave de API. Verifique 'config.properties' na raiz do projeto.");
            return;
        }

        Scanner leitura = new Scanner(System.in);
        int opcao = -1;

        System.out.println("**************************************");
        System.out.println("Seja Bem-vindo ao Conversor de Moeda!");
        System.out.println("**************************************");

        while (opcao != 0) {
            System.out.println("\nSelecione a opção de conversão:");
            System.out.println("1. BRL -> USD");
            System.out.println("2. USD -> BRL");
            System.out.println("3. BRL -> EUR");
            System.out.println("4. EUR -> BRL");
            System.out.println("5. BRL -> GBP");
            System.out.println("6. GBP -> BRL");
            System.out.println("0. Sair");
            System.out.println("--------------------------------------");
            System.out.print("Digite: ");

            try {
                opcao = leitura.nextInt();

                if (opcao == 0) {
                    System.out.println("Até a próxima!");
                    break;
                }

                if (opcao >= 1 && opcao <= 6) {
                    System.out.print("Digite o valor a ser convertido: ");
                    double valorParaConverter = leitura.nextDouble();

                    String moedaOrigem = "";
                    String moedaDestino = "";

                    switch (opcao) {
                        case 1:
                            moedaOrigem = "BRL";
                            moedaDestino = "USD";
                            break;
                        case 2:
                            moedaOrigem = "USD";
                            moedaDestino = "BRL";
                            break;
                        case 3:
                            moedaOrigem = "BRL";
                            moedaDestino = "EUR";
                            break;
                        case 4:
                            moedaOrigem = "EUR";
                            moedaDestino = "BRL";
                            break;
                        case 5:
                            moedaOrigem = "BRL";
                            moedaDestino = "GBP";
                            break;
                        case 6:
                            moedaOrigem = "GBP";
                            moedaDestino = "BRL";
                            break;
                    }

                    TaxasCambio taxas = obterTaxas(moedaOrigem);

                    if (taxas != null && taxas.getConversion_rates() != null) {
                        Map<String, Double> conversoes = taxas.getConversion_rates();

                        System.out.println("Moeda destino solicitada: " + moedaDestino);
                        System.out.println("Moedas disponíveis na resposta da API: " + conversoes.keySet());

                        if (conversoes.containsKey(moedaDestino)) {
                            Double taxaDeCambio = conversoes.get(moedaDestino);
                            double valorConvertido = valorParaConverter * taxaDeCambio;
                            System.out.printf("Valor convertido de %s para %s: %.2f%n",
                                    moedaOrigem, moedaDestino, valorConvertido);
                        } else {
                            System.out.println("Erro: Moeda de destino '" + moedaDestino + "' não encontrada .");
                        }
                    } else {
                        System.out.println("Erro: Taxas de câmbio não foram carregadas.");
                    }

                } else {
                    System.out.println("Opção inválida. Tente novamente.");
                }

            } catch (InputMismatchException e) {
                System.out.println("Entrada inválida. Digite um número válido.");
                leitura.nextLine();
            }
        }

        leitura.close();
    }
}
