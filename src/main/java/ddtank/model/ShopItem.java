package ddtank.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "Item")
public class ShopItem {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private int id;

    @JacksonXmlProperty(isAttribute = true, localName = "Name")
    private String name;

    @JacksonXmlProperty(isAttribute = true, localName = "Price")
    private int price;

    public ShopItem(int id, String name, int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getters e Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
}