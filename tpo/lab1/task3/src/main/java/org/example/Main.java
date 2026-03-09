package org.example;

import org.example.commands.AcceptFateCommand;
import org.example.commands.DescribePositionCommand;
import org.example.commands.RealizeExistenceCommand;
import org.example.controllers.Scenario;
import org.example.models.Whale;

public class Main {

    public static void main(String[] args) {
        var whale = new Whale("Кит");

        var scenario = new Scenario();
        scenario.addCommand(new DescribePositionCommand(whale));
        scenario.addCommand(new RealizeExistenceCommand(whale));
        scenario.addCommand(new AcceptFateCommand(whale));

        scenario.execute();
    }
}