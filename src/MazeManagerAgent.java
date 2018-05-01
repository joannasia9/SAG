import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MazeManagerAgent extends Agent{

    public static final int MAZE_HEIGHT = 99;
    public static final int MAZE_WIDTH = 99;

    private MazeField[][] maze;

    AID aid;

    public void setup(){
        aid = new AID();

        maze = generateMaze();

        DFAgentDescription dfad = new DFAgentDescription();
        dfad.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("maze");
        sd.setName("manager");
        dfad.addServices(sd);

        try {
            DFService.register(this, dfad);
        }
        catch (FIPAException ex) {
            System.out.println(ex.getMessage());
        }

        addBehaviour(new TickerBehaviour(this, 500) {
            @Override
            protected void onTick() {
                updatePheromons();
            }
        });

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                // TBD
                // Update maze - moving walls
            }
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage receivedMessage = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

                if (receivedMessage != null) {

                    try {
                        Command cmd = (Command) receivedMessage.getContentObject();

                        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                        message.addReceiver(receivedMessage.getSender());

                        switch (cmd.getCommandCode()) {
                            case MAZE_REQUEST:
                                MazeInformCommand command = new MazeInformCommand();
                                command.setMazeValues(maze);

                                try {
                                    message.setContentObject(command);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                                System.out.println(Command.CommandCode.MAZE_REQUEST.toString());
                                send(message);
                                break;

                            case ANT_NEIGHBORHOOD_REQUEST:
                                AntNeighbourhoodRequestCommand antNeighborhoodRequestCommand = (AntNeighbourhoodRequestCommand) receivedMessage.getContentObject();
                                AntNeighbourhoodInformCommand neighbourhoodInformCommand = new AntNeighbourhoodInformCommand();

                                Point antPosition = antNeighborhoodRequestCommand.getCurrentPosition();

                                //Sending information about four neighbouring fields: northern, eastern, southern, western
                                //(in that order):
                                Vector<MazeField> neighbourhood = new Vector<MazeField>(4);
                                for(int i = -1; i < 2; i+=2) {
                                    if(antPosition.x + i > 0 && antPosition.x + i < MAZE_HEIGHT)
                                        if (maze[antPosition.x + i][antPosition.y].getValue() != MazeField.FieldCode.WALL &&
                                                maze[antPosition.x + i][antPosition.y].getValue() != MazeField.FieldCode.MOBILE_WALL)
                                            neighbourhood.add(maze[antPosition.x + i][antPosition.y]);

                                    if(antPosition.y + i > 0 && antPosition.y + i < MAZE_WIDTH)
                                        if (maze[antPosition.x][antPosition.y + i].getValue() != MazeField.FieldCode.WALL &&
                                                maze[antPosition.x][antPosition.y +i].getValue() != MazeField.FieldCode.MOBILE_WALL)
                                            neighbourhood.add(maze[antPosition.x][antPosition.y + i]);
                                }
                                neighbourhoodInformCommand.setMazeValues(neighbourhood);
                                try {
                                    message.setContentObject(neighbourhoodInformCommand);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                                System.out.println(Command.CommandCode.ANT_NEIGHBORHOOD_REQUEST.toString());
                                send(message);

                                break;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                else{
                    block();
                }
            }
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage receivedMessage = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));

                if (receivedMessage != null) {

                    try{
                        Command cmd = (Command)receivedMessage.getContentObject();

                        switch (cmd.getCommandCode())
                        {
                            case ANT_POSITION_INFORM:
                                AntPositionInformCommand positionInformCommand = (AntPositionInformCommand) receivedMessage.getContentObject();

                                Point newPosition = positionInformCommand.getNewPosition();
                                int antDistance = positionInformCommand.getDistance();
                                maze[newPosition.x][newPosition.y].setValue(MazeField.FieldCode.ANT);
                                maze[newPosition.x][newPosition.y].setPheromonePower(maze[newPosition.x][newPosition.y].getPheromonePower()+1.0/(double)antDistance);
                                System.out.println(Command.CommandCode.ANT_POSITION_INFORM.toString());
                                break;
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }

                } else {
                    block();
                }
            }
        });
    }

    public String generateMazeString(MazeField[][] mazeInt){
        StringBuilder mazeString = new StringBuilder();
        for(int i=0; i<mazeInt.length; i++){
            for(int j=0; j<mazeInt.length; j++){
                mazeString.append(mazeInt[i][j]).append("\t");
            }
            mazeString.append("\n");
        }
        return mazeString.toString();
    }

    public void takeDown(){
        try { DFService.deregister(this); }
        catch (Exception ex) { ex.printStackTrace();}
    }

    public MazeField[][] generateMaze() {
        maze = new MazeField[MAZE_HEIGHT][MAZE_WIDTH];

        // Initialize
        for (int i = 0; i < MAZE_HEIGHT; i++)
            for (int j = 0; j < MAZE_WIDTH; j++) {
                maze[i][j] = new MazeField(MazeField.FieldCode.WALL);
                maze[i][j].setCoordinateX(i);
                maze[i][j].setCoordinateY(j);
            }

        Random rand = new Random();
        // r for row、c for column
        // Generate random r
        int r = rand.nextInt(MAZE_HEIGHT);
        while (r % 2 == 0) {
            r = rand.nextInt(MAZE_HEIGHT);
        }
        // Generate random c
        int c = rand.nextInt(MAZE_WIDTH);
        while (c % 2 == 0) {
            c = rand.nextInt(MAZE_WIDTH);
        }
        // Starting cell
        maze[r][c] = new MazeField(MazeField.FieldCode.ALLEY);

        //　Allocate the maze with recursive method
        recursion(r, c);

        //select exits
        createExits(maze);

        return maze;
    }

    private void recursion(int r, int c) {
        // 4 random directions
        int[] randDirs = generateRandomDirections();

        // Examine each direction
        for (int i = 0; i < randDirs.length; i++) {

            switch(randDirs[i]){
                case 1: // Up
                    //　Whether 2 cells up is out or not
                    if (r-2 <= 0)
                        continue;
                    if (maze[r - 2][c].getValue() != MazeField.FieldCode.ALLEY) {
                        maze[r-2][c].setValue(MazeField.FieldCode.ALLEY);
                        maze[r-1][c].setValue(MazeField.FieldCode.ALLEY);
                        maze[r-2][c].setPheromonePower(0.1);
                        maze[r-1][c].setPheromonePower(0.1);
                        recursion(r - 2, c);
                    }
                    break;
                case 2: // Right
                    // Whether 2 cells to the right is out or not
                    if (c + 2 >= MAZE_WIDTH - 1)
                        continue;
                    if (maze[r][c + 2].getValue() != MazeField.FieldCode.ALLEY) {
                        maze[r][c + 2].setValue(MazeField.FieldCode.ALLEY);
                        maze[r][c + 1].setValue(MazeField.FieldCode.ALLEY);
                        maze[r][c + 2].setPheromonePower(0.1);
                        maze[r][c + 1].setPheromonePower(0.1);
                        recursion(r, c + 2);
                    }
                    break;
                case 3: // Down
                    // Whether 2 cells down is out or not
                    if (r + 2 >= MAZE_HEIGHT - 1)
                        continue;
                    if (maze[r + 2][c].getValue() != MazeField.FieldCode.ALLEY) {
                        maze[r+2][c].setValue(MazeField.FieldCode.ALLEY);
                        maze[r+1][c].setValue(MazeField.FieldCode.ALLEY);
                        maze[r+2][c].setPheromonePower(0.1);
                        maze[r+1][c].setPheromonePower(0.1);
                        recursion(r + 2 , c);
                    }
                    break;
                case 4: // Left
                    // Whether 2 cells to the left is out or not
                    if (c - 2 <= 0)
                        continue;

                    if (maze[r][c - 2].getValue() != MazeField.FieldCode.ALLEY) {
                        maze[r][c - 2].setValue(MazeField.FieldCode.ALLEY);
                        maze[r][c - 1].setValue(MazeField.FieldCode.ALLEY);
                        maze[r][c - 2].setPheromonePower(0.1);
                        maze[r][c - 1].setPheromonePower(0.1);
                        recursion(r, c - 2);
                    }
                    break;
            }
        }

    }

    /**
     * Generate an array with random directions 1-4
     * @return Array containing 4 directions in random order
     */

    private int[] generateRandomDirections() {
        ArrayList<Integer> randoms = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            randoms.add(i + 1);
        Collections.shuffle(randoms);

        int [] tab = new int[4];
        for (int i = 0; i<randoms.size(); i++){
            tab[i]=randoms.get(i);
        }
        return tab;
    }

    private void createExits(MazeField[][] maze){
        Boolean ok = false;

        while(!ok) {
            Random rand = new Random();
            int randomedColumn = rand.nextInt(MAZE_WIDTH-10)+2;
            randomedColumn+=2;
            if(randomedColumn != maze.length-1){
                if(maze[maze.length-2][randomedColumn].getValue() == MazeField.FieldCode.ALLEY
                        && maze[maze.length-2][randomedColumn-1].getValue() == MazeField.FieldCode.ALLEY
                        &&maze[maze.length-2][randomedColumn+1].getValue() == MazeField.FieldCode.ALLEY) {
                    maze[maze.length-1][randomedColumn].setValue(MazeField.FieldCode.EXIT);
                    maze[maze.length-1][randomedColumn-1].setValue(MazeField.FieldCode.EXIT);
                    ok = true;
                }

            }
        }

    }

    private void updatePheromons()
    {
        for (int i = 0; i < maze.length; ++i) {
            for (int j = 0; j < maze[0].length; ++j) {
                if(maze[i][j].getValue() == MazeField.FieldCode.ANT || maze[i][j].getValue() == MazeField.FieldCode.ALLEY)
                {
                   maze[i][j].setPheromonePower(maze[i][j].getPheromonePower()*(1-MazeField.EVAPORATION_COEFF));
                }
            }
        }
    }
}
