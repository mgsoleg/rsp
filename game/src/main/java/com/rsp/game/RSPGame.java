package com.rsp.game;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class RSPGame {

    private int[][] markovModel;
    private static final Random random = new Random();
    private int userWin;
    private int userLoose;
    private int tie;
    private RSP lastMachineChoice;
    private RSP lastUserChoice;

    private RoundResults lastUserRoundResult;


    public RSPGame() {
        int numberOfChoices = RSP.values().length;
        markovModel = new int[numberOfChoices][numberOfChoices];
    }


    public RSP play(Optional<RSP> choice) {
        if (choice.isPresent()) {
            return play(choice.get());
        }
        final RSP responseFirstTime = RSP.values()[random.nextInt(RSP.values().length)];
        lastMachineChoice = responseFirstTime;
        List<RSP> looseTo = responseFirstTime.looseTo;
        lastUserChoice = looseTo.get(random.nextInt(looseTo.size()));
        return responseFirstTime;
    }

    private RSP play(RSP currentUserChoice) {


        markovModel[lastUserChoice.ordinal()][currentUserChoice.ordinal()]++;
        updateStatistics(currentUserChoice);

        final RSP nextMachineChoice = getNextMachineChoice(currentUserChoice);
        lastMachineChoice = nextMachineChoice;
        return nextMachineChoice;
    }

    public int getUserWin() {
        return userWin;
    }

    public int getUserLoose() {
        return userLoose;
    }

    public int getTie() {
        return tie;
    }

    public RSP getLastMachineChoice() {
        return lastMachineChoice;
    }

    private void updateStatistics(RSP currentUserChoice) {
        if (currentUserChoice == lastMachineChoice) {
            tie++;
            lastUserRoundResult = RoundResults.TIE;
        } else if (currentUserChoice.looseTo.contains(lastMachineChoice)) {
            userLoose++;
            lastUserRoundResult = RoundResults.LOOSE;
        } else {
            userWin++;
            lastUserRoundResult = RoundResults.WON;
        }
        this.lastUserChoice = currentUserChoice;
    }

    private RSP getNextMachineChoice(RSP currentUserChoice) {

        int mostProbable = 0;
        int currentUserChoiceIndex = currentUserChoice.ordinal();

        for (int i = 0; i < RSP.values().length; i++) {

            if (markovModel[currentUserChoiceIndex][i] > markovModel[currentUserChoiceIndex][mostProbable]) {
                mostProbable = i;
            }
        }

        final RSP userMostProbableChoice = RSP.values()[mostProbable];
        List<RSP> looseTo = userMostProbableChoice.looseTo;
        return looseTo.get(random.nextInt(looseTo.size()));
    }

    public RoundResults getLastUserRoundResult() {
        return lastUserRoundResult ;
    }
}
