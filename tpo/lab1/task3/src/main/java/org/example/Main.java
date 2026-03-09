package org.example;

import org.example.commands.BeginStory;
import org.example.commands.CreaturePart;
import org.example.commands.LoseIdentityPart;
import org.example.commands.Scenario;
import org.example.commands.TextPart;
import org.example.models.Whale;

import static org.example.models.WhaleCondition.USUAL;

public class Main {
    public static void main(String[] args) {
        Whale whale = new Whale("кит", USUAL);

        Scenario scenario = new Scenario()
                .addCommand(new BeginStory(whale))
                .addCommand(new CreaturePart(whale))
                .addCommand(new TextPart(", перед тем, как ему пришлось свыкнуться с осознанием того, что оно уже больше не кит."));

        System.out.println(scenario.execute());
    }
}