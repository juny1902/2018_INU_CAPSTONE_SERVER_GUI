package application;
	
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.w3c.dom.NodeList;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


public class Main extends Application {
	@FXML
	private TextField tf_server_status;
	@FXML
	private TextField tf_req_bus_plate; //
	@FXML
	private TextField tf_req_stat_name; 
	@FXML
	private TextField tf_req_stat_seq; //
	@FXML
	private TextField tf_req_bus_num; //
	@FXML
	private TextField tf_run_bus_num;
	@FXML
	private TextArea ta_running_log; // 
	@FXML
	private TextField tf_req_function;

	
	public void serverStart() throws MqttException {
		MqttClient client = new MqttClient("tcp://iot.eclipse.org:1883", MqttClient.generateClientId());
		client.setCallback(new MqttCallback() {

			@Override
			public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
				// TODO Auto-generated method stub
				String[] msg = arg1.toString().split("&");
				String selection = msg[0];
				String busSeq = msg[1]; // 30 
				String routeId = msg[2];
				String busPlateNo = msg[3]; // 12<sk 7888
				String busNumber = msg[4];
				String selStationName = msg[5];
				String userType = msg[6];

				tf_req_bus_plate.setText(busPlateNo);
				tf_req_stat_seq.setText(busSeq);
				tf_req_bus_num.setText(busNumber);
				tf_req_stat_name.setText(selStationName);

				tf_run_bus_num.setText(busNumber);
				if(selection.equals("geton")){
					tf_req_function.setText("Get On");
				}else if(selection.equals("getoff")) {
					tf_req_function.setText("Get Off");
				}else{
					tf_req_function.setText("Awaiting...");
				}
				// System.out.printf("Selection : %s\nSeq : %s\nRoute : %s\nPlate : %s\n", selection, busSeq, routeId, busPlateNo);
				String uri = "http://openapi.gbis.go.kr/ws/rest/buslocationservice?serviceKey=i%2FmgmkmoCSBv8EUR8Jv1%2FTOw767UUNZEI%2FSGQnCmnDSb4kM1Vty5Dsqlw%2Bcx%2B8o%2FtfNUzA7PNyaMnqVHCMqD8A%3D%3D&routeId="
						+ routeId;
				boolean waitArrived = true;
				
				boolean goRas_geton = false; //  subscribe message duplication prohibition
				boolean goRas_getend = false; //  subscribe message duplication prohibition
				boolean goRas_getoff = false; //  subscribe message duplication prohibition
				boolean goRas_getoffend = false; //  subscribe message duplication prohibition
			
				if (selection.equals("geton")) {
					
					while (waitArrived) {
						
						org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
								.parse(uri);
						XPath xpath = XPathFactory.newInstance().newXPath();
						NodeList plateNoList = (NodeList) xpath.evaluate("//busLocationList/plateNo", document,
								XPathConstants.NODESET);
						NodeList stationSeqList = (NodeList) xpath.evaluate("//busLocationList/stationSeq", document,
								XPathConstants.NODESET);

						//System.out.println("geton_plate length: "+plateNoList.getLength());
						//System.out.println("geton_station length: "+stationSeqList.getLength());
						String str_log = "";
						for (int i = 0; i < plateNoList.getLength(); i++) {
							str_log += (plateNoList.item(i).getTextContent() + "..." + stationSeqList.item(i).getTextContent() + "\n");
							
							if ((Integer.parseInt(stationSeqList.item(i).getTextContent()) == Integer.parseInt(busSeq)-1) && (plateNoList.item(i).getTextContent().equals(busPlateNo))) {
								if(goRas_geton == false) {
									// System.out.println("geton_Send to Raspberry On1 : " + busPlateNo);
									str_log += (busPlateNo + " has arrived the station #"+stationSeqList.item(i).getTextContent()+"\n");
									str_log += ("Send \"Get On\" message to the raspberry pi device." + "\n");
									String res_msg = "geton&" + busPlateNo + "&" + selStationName;

									res_msg +=  "&" + userType;

									client.publish("bus_responses",res_msg.getBytes(),1,false);
									// System.out.println("Blocked");
									goRas_geton = true;
									break;
								}
							} 
						}
						ta_running_log.setText(str_log);
						
						for (int i = 0; i < plateNoList.getLength(); i++) {
							if((Integer.parseInt(stationSeqList.item(i).getTextContent()) >= Integer.parseInt(busSeq)) && (plateNoList.item(i).getTextContent().equals(busPlateNo))) {
								if(goRas_getend == false) {
									//System.out.println("geton_Send to Raspberry On2 : " + busPlateNo);
									String res_msg = "geton_end&" + busPlateNo + "&" + selStationName;

									res_msg +=  "&" + userType;

									client.publish("bus_responses",res_msg.getBytes(),1,false);
									waitArrived = false;
									goRas_getend = true;
								}
							}
						}
						Thread.sleep(10000);
					}
					//System.out.println("geton_While escaped");
				} else if(selection.equals("getoff")) {
					while (waitArrived) {
						org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
								.parse(uri);
						XPath xpath = XPathFactory.newInstance().newXPath();
						NodeList plateNoList = (NodeList) xpath.evaluate("//busLocationList/plateNo", document,
								XPathConstants.NODESET);
						NodeList stationSeqList = (NodeList) xpath.evaluate("//busLocationList/stationSeq", document,
								XPathConstants.NODESET);

						// System.out.println("getoff_plate length: "+plateNoList.getLength());
						// System.out.println("getoff_station length: "+stationSeqList.getLength());
						String str_log = "";
						for (int i = 0; i < plateNoList.getLength(); i++) {
							str_log += (plateNoList.item(i).getTextContent() + "..." + stationSeqList.item(i).getTextContent() + "\n");
							if ((Integer.parseInt(stationSeqList.item(i).getTextContent()) == Integer.parseInt(busSeq)-1) && (plateNoList.item(i).getTextContent().equals(busPlateNo))) {
								if(goRas_getoff == false) {
									str_log += (busPlateNo + " has arrived the station #"+stationSeqList.item(i).getTextContent()+"\n");
									str_log += ("Send \"Get On\" message to the raspberry pi device." + "\n");
									// System.out.println("getoff_Send to Raspberry On1 : " + busPlateNo);
									String res_msg = "getoff&" + busPlateNo + "&" + selStationName;

									res_msg +=  "&" + userType;

									client.publish("bus_responses",res_msg.getBytes(),1,false);
									// System.out.println("Blocked");
									goRas_getoff = true;
									break;
								}
							} 
						}
						ta_running_log.setText(str_log);
						
						for (int i = 0; i < plateNoList.getLength(); i++) {
							if((Integer.parseInt(stationSeqList.item(i).getTextContent()) >= Integer.parseInt(busSeq)) && (plateNoList.item(i).getTextContent().equals(busPlateNo))) {
								if(goRas_getoffend == false) {
									//System.out.println("getoff_Send to Raspberry On2 : " + busPlateNo);
									String res_msg = "getoff_end&" + busPlateNo + "&" + selStationName;

									res_msg +=  "&" + userType;

									client.publish("bus_responses",res_msg.getBytes(),1,false);
									waitArrived = false;
									goRas_getoffend = true;
								}
							}
						}
						

						Thread.sleep(10000);
					}
					//System.out.println("getoff_While escaped");
				}
				
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken arg0) {
				// TODO Auto-generated method stub paho10742599539225

			}

			@Override
			public void connectionLost(Throwable arg0) {
				tf_server_status.setText(String.format("Disconnected.",client.getClientId(), client.getServerURI()));

			}
		});
		client.connect();
		tf_req_function.setText("Awaiting...");
		tf_server_status.setText(String.format("[%s]Connection Established. - %s",client.getClientId(), client.getServerURI()));
		client.subscribe("bus_request");

	}

	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("capstone_gui.fxml"));
			
			loader.setController(this);
			Parent root = (Parent) loader.load();
			
			Scene scene = new Scene(root, 800, 500);
	        primaryStage.setScene(scene);
	        primaryStage.show();
			serverStart();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
