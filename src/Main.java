import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main
{
    private String nick;

    private final static String HOST = "localhost";
    private final static int PORT = 1500;

    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private boolean validation = false;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        Main main = new Main();
        var socket = new Socket(HOST, PORT);

        main.oos = new ObjectOutputStream(socket.getOutputStream());
        main.oos.flush();
        main.ois = new ObjectInputStream(socket.getInputStream());

        main.startChat();
    }

    private void startChat() throws IOException, InterruptedException
    {
        new Thread(this::startListening).start();
        validate();
        start();
    }

    private void validate() throws IOException, InterruptedException
    {
        do
        {
            System.out.println("Enter your nick");
            this.nick = new Scanner(System.in).nextLine();

            Message validateMsg = new Message.MessageBuilder()
                    .validate()
                    .sender(this.nick)
                    .build();
            this.oos.writeObject(validateMsg);

            Thread.sleep(500);

        } while (!validation);
    }

    private void startListening()
    {

        while (true)
        {
            try
            {
                Message msgReceived = (Message) this.ois.readObject();

                if (msgReceived.getNickAccepted())
                {
                    System.out.println(msgReceived.getMessage());
                    this.validation = true;
                } else if (msgReceived.getRecipient() == null && msgReceived.getFile() == null)
                    System.out.println(msgReceived.getSender() + ": " + msgReceived.getMessage());

                else if (msgReceived.getRecipient() != null && !msgReceived.getNickAccepted() && msgReceived.getFile() == null)
                    System.out.println("Message from: " + msgReceived.getSender() + ": " + msgReceived.getMessage());

                else if (msgReceived.getFile() != null)
                    receiveFile(msgReceived.getMessage(), msgReceived.getFile());
            } catch (IOException | ClassNotFoundException e)
            {
                e.printStackTrace();
                break;
            }
        }
    }

    private byte[] getFile(String filepath) throws IOException
    {
        return Files.readAllBytes(Paths.get(filepath));
    }

    private void receiveFile(String name, byte... file) throws IOException
    {
        try (FileOutputStream fos = new FileOutputStream("_" + name))
        {
            fos.write(file);
        }
        System.out.println("File has been received");
    }

    private void start() throws IOException
    {
        System.out.println("Welcome " + this.nick);

        while (true)
        {
            var line = new Scanner(System.in).nextLine();

            if (line.equals("#exit"))
            {
                quit();
                break;
            }

            Message.MessageBuilder msg = new Message.MessageBuilder()
                    .sender(this.nick);

            if (line.split(": ").length == 2)
            {
                msg.message(line.split(": ")[1])
                        .recipient(line.split(": ")[0]);
            } else if (line.split(": ").length == 3)
            {
                msg.recipient(line.split(": ")[0])
                        .message(line.split(": ")[2])
                        .file(this.getFile(line.split(": ")[2]));
            } else
                msg.message(line);

            Message msgToSend = msg.build();
            this.oos.writeObject(msgToSend);
        }
    }

    private void quit()
    {
        System.exit(0);
    }
}