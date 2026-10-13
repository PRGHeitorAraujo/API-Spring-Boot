package dio.budgeting.domain;

/**
 * Categorias de gasto. Cada uma tem um rótulo em português,
 * usado nas respostas e na documentação para a IA.
 */
public enum Category {
    GROCERIES("Mercado"),
    PHARMA("Farmácia"),
    AUTO("Carro"),
    TRANSPORT("Transporte"),
    RESTAURANT("Restaurante"),
    LEISURE("Lazer"),
    HOUSING("Moradia"),
    OTHER("Outros");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
