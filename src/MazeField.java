import java.io.Serializable;

public class MazeField implements Serializable {

    public static final double EVAPORATION_COEFF = 0.2;

    public enum FieldCode {
        WALL, EXIT, MOBILE_WALL, ANT, ALLEY
    }

    private FieldCode value;
    private int coordinateX, coordinateY;
    private double pheromonePower;

    MazeField(FieldCode value)
    {
        this.value = value;
        if(value == FieldCode.ALLEY || value == FieldCode.ANT){
            this.pheromonePower = 0.1;
        }
        else {
            this.pheromonePower = 0;
        }
    }

    public FieldCode getValue()
    {
        return value;
    }

    public void setValue(FieldCode value)
    {
        this.value = value;
    }

    public void setPheromonePower(double power)
    {
        this.pheromonePower = power;
    }

    public double getPheromonePower()
    {
        return this.pheromonePower;
    }

    public void setCoordinateX(int x)
    {
        this.coordinateX = x;
    }

    public int getCoordinateX()
    {
        return this.coordinateX;
    }

    public void setCoordinateY(int y)
    {
        this.coordinateY = y;
    }

    public int getCoordinateY()
    {
        return this.coordinateY;
    }

    public void evaporatePheromone(){this.pheromonePower *= (1-EVAPORATION_COEFF);}
}


