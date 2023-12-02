package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.protobuf.Empty; // needed to use Empty


/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class Client {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final SortGrpc.SortBlockingStub blockingStubSort;
  private final EncryptionGrpc.EncryptionBlockingStub blockingStubEncryption;
  private final WeatherGrpc.WeatherBlockingStub blockingStubWeather;
  

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);
    blockingStubSort = SortGrpc.newBlockingStub(channel);
    blockingStubEncryption = EncryptionGrpc.newBlockingStub(channel);
    blockingStubWeather = WeatherGrpc.newBlockingStub(channel);


  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;
    blockingStubSort = SortGrpc.newBlockingStub(channel);
    blockingStubEncryption = EncryptionGrpc.newBlockingStub(channel);
    blockingStubWeather = WeatherGrpc.newBlockingStub(channel);

  }

  public void askServerToParrot(String message) {

    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response;
    try {
      response = blockingStub.parrot(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    System.out.println("Received from server: " + response.getMessage());
  }

  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response;

    // just to show how to use the empty in the protobuf protocol
    Empty empt = Empty.newBuilder().build();

    try {
      response = blockingStub2.getJoke(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }





  //Two new methods for Task 1
  public void sortNumbers(List<Integer> numbers, Algo algo) {
    SortRequest request = SortRequest.newBuilder().addAllData(numbers).setAlgo(algo).build();
    SortResponse response;
    try {
      response = blockingStubSort.sort(request);
      if (response.getIsSuccess()) {
        System.out.println("Sorted numbers: " + response.getDataList());
      } else {
        System.err.println("Sorting failed: " + response.getError());
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void encryptText(String text) {
    EncryptRequest request = EncryptRequest.newBuilder().setInput(text).build();
    EncryptResponse response;
    try {
      response = blockingStubEncryption.encrypt(request);
      System.out.println("Encrypted text: " + response.getSolution());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }
  public void decryptText(String encryptedText) {
    DecryptRequest request = DecryptRequest.newBuilder().setInput(encryptedText).build();
    DecryptResponse response;
    try {
      response = blockingStubEncryption.decrypt(request);
      System.out.println("Decrypted text: " + response.getSolution());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }



    //Methods for created Weather Forecast service

  public void getCurrentWeather(String location) {
    WeatherRequest request = WeatherRequest.newBuilder().setLocation(location).build();
    WeatherResponse response;
    try {
      response = blockingStubWeather.getCurrentWeather(request);
      System.out.println("Current weather: " + response.getDescription() + ", " + response.getTemperature() + " degrees");
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }
  
  public void getWeatherForecast(String location) {
    WeatherRequest request = WeatherRequest.newBuilder().setLocation(location).build();
    WeatherForecastResponse response;
    try {
      response = blockingStubWeather.getWeatherForecast(request);
      System.out.println("Weather forecast:");
      for (WeatherResponse forecast : response.getForecastList()) {
        System.out.println(forecast.getDescription() + ", " + forecast.getTemperature() + " degrees");
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }








  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 7) {
      System.out
          .println("Expected arguments: <host(String)> <port(int)> <regHost(string)> <regPort(int)> <message(String)> <regOn(bool)>");
      System.exit(1);
    }
    int port = 9099;
    int regPort = 9003;
    String host = args[0];
    String regHost = args[2];
    String message = args[4];
    int auto = Integer.parseInt(args[6]);
    try {
      port = Integer.parseInt(args[1]);
      regPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be an integer");
      System.exit(2);
    }

    // Create a communication channel to the server (Node), known as a Channel. Channels
    // are thread-safe
    // and reusable. It is common to create channels at the beginning of your
    // application and reuse
    // them until the application shuts down.
    String target = host + ":" + port;
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS
        // to avoid
        // needing certificates.
        .usePlaintext().build();

    String regTarget = regHost + ":" + regPort;
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regTarget).usePlaintext().build();
    try {

      // ##############################################################################
      // ## Assume we know the port here from the service node it is basically set through Gradle
      // here.
      // In your version you should first contact the registry to check which services
      // are available and what the port
      // etc is.

      /**
       * Your client should start off with 
       * 1. contacting the Registry to check for the available services
       * 2. List the services in the terminal and the client can
       *    choose one (preferably through numbering) 
       * 3. Based on what the client chooses
       *    the terminal should ask for input, eg. a new sentence, a sorting array or
       *    whatever the request needs 
       * 4. The request should be sent to one of the
       *    available services (client should call the registry again and ask for a
       *    Server providing the chosen service) should send the request to this service and
       *    return the response in a good way to the client
       * 
       * You should make sure your client does not crash in case the service node
       * crashes or went offline.
       */

      // Just doing some hard coded calls to the service node without using the
      // registry
      // create client
      Client client = new Client(channel, regChannel);

      // Contact the Registry to check for the available services
      client.getServices();
      int serviceNumber;
      // List the services in the terminal and the client can choose one
      do {
        if (auto == 1) {
          System.out.println("Auto is on!");
          //Do every case with automated parameters
          System.out.println("Echoing: Parrot");
          client.askServerToParrot("Parrot");
          System.out.println("Getting 2 jokes");
          client.askForJokes(2);
          System.out.println("Sorting numbers 5, 4, 8, 1, 2 with merge sort");
          client.sortNumbers(Arrays.asList(5, 4, 8, 1, 2), service.Algo.MERGE);
          System.out.println("Sorting numbers 5, 4, 8, 1, 2 with quick sort");
          client.sortNumbers(Arrays.asList(5, 4, 8, 1, 2), service.Algo.QUICK);
          System.out.println("Sorting numbers 5, 4, 8, 1, 2 with default sort");
          client.sortNumbers(Arrays.asList(5, 4, 8, 1, 2), service.Algo.INTERN);
          System.out.println("Encrypting text: Hello");
          client.encryptText("Hello");
          System.out.println("Decrypting text: TNc5oGMut0nM+GtofqS4sQ==");
          client.decryptText("TNc5oGMut0nM+GtofqS4sQ==");
          System.out.println("Getting current weather for London");
          client.getCurrentWeather("London");
          System.out.println("Getting weather forecast for London");
          client.getWeatherForecast("London");
          serviceNumber = 0;
          System.out.println("Exiting...");
          break;
        } 
      
      System.out.println("Choose a service by entering its number (0 to exit, 1 for Echo, 2 for Joke, 3 for Sort, 4 for Encryption, 5 for Decryption, 6 for Current Weather, 7 for Weather Forecast):");      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      serviceNumber = Integer.parseInt(reader.readLine());

      // Based on what the client chooses, the terminal should ask for input
      switch (serviceNumber) {
        case 0:
            System.out.println("Exiting...");
            break;
        case 1:
          System.out.println("Enter a sentence:");
          String sentence = reader.readLine();
          client.askServerToParrot(sentence);
          break;
        case 2:
          System.out.println("How many jokes would you like?");
          int num = Integer.parseInt(reader.readLine());
          client.askForJokes(num);
          break;
        case 3:
          System.out.println("Enter numbers to sort (separated by spaces):");
          List<Integer> numbers = Arrays.stream(reader.readLine().split(" "))
              .map(Integer::parseInt)
              .collect(Collectors.toList());
          System.out.println("Choose a sorting algorithm (1 for MERGE, 2 for QUICK, 3 for INTERN):");
          int algoNumber = Integer.parseInt(reader.readLine());
          service.Algo algo;
          switch (algoNumber) {
            case 1:
              algo = service.Algo.MERGE;
              break;
            case 2:
              algo = service.Algo.QUICK;
              break;
            case 3:
              algo = service.Algo.INTERN;
              break;
            default:
              System.out.println("Invalid algorithm number.");
              return;
          }
          client.sortNumbers(numbers, algo);
          break;
        case 4:
          System.out.println("Enter text to encrypt:");
          String text = reader.readLine();
          client.encryptText(text);
          break;
        case 5:
          System.out.println("Enter text to decrypt:");
          String textToDecrypt = reader.readLine();
          client.decryptText(textToDecrypt);
          break;
        case 6:
          System.out.println("Enter a location to get the current weather:");
          String location = reader.readLine();
          System.out.println("Getting current weather for " + location + "...");
          client.getCurrentWeather(location);
          break;
        case 7:
          System.out.println("Enter a location to get the weather forecast:");
          String location2 = reader.readLine();
          System.out.println("Getting weather forecast for " + location2 + "...");
          client.getWeatherForecast(location2);
          break;
        default:
          System.out.println("Invalid service number.");
          break;
      }
      } while (serviceNumber != 0);
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent
      // leaking these
      // resources the channel should be shut down when it will no longer be used. If
      // it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
