package server.beans;

import jakarta.inject.Named;

@Named("firstBean")
public class ExampleBean {
    private String name;

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }
}
