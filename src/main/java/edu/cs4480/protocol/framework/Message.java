package edu.cs4480.protocol.framework;

public class Message
{
    private String data;
    
    public Message(String inputData)
    {
        if (inputData == null)
        {
            data = "";
        }
        else if (inputData.length() > NetworkSimulator.MAX_DATA_SIZE)
        {
            data = "";
        }
        else
        {
            data = inputData;
        }
    }
           
    public boolean setData(String inputData)
    {
        if (inputData == null)
        {
            data = "";
            return false;
        }
        else if (inputData.length() > NetworkSimulator.MAX_DATA_SIZE)
        {
            data = "";
            return false;
        }
        else
        {
            data = inputData;
            return true;
        }
    }
    
    public String getData()
    {
        return data;
    }
}
