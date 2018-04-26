package sample;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.bean.WxCpMessage;
import me.chanjar.weixin.cp.config.WxCpInMemoryConfigStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.model.MonitorModel;
import sample.model.PortModel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller implements Initializable, Runnable {
    private static Logger LOGGER = LoggerFactory.getLogger(Controller.class);
    public ProgressBar process;
    public TableView<PortModel> table;
    public TableColumn<PortModel, String> ip;
    public TableColumn<PortModel, Integer> port;
    public TableColumn<PortModel, String> errorMsg;

    private MonitorModel monitorModel = new MonitorModel();
    private WxCpServiceImpl wxCpService;
    private SimpleDoubleProperty processValue = new SimpleDoubleProperty(0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            process.progressProperty().bind(processValue);
            ip.setCellValueFactory(new PropertyValueFactory<>("ip"));
            port.setCellValueFactory(new PropertyValueFactory<>("port"));
            errorMsg.setCellValueFactory(new PropertyValueFactory<>("errorMsg"));

            monitorModel = MonitorModel.load("config.json");
            table.getItems().setAll(monitorModel.getPortList());

            WxCpInMemoryConfigStorage config = new WxCpInMemoryConfigStorage();
            config.setCorpId(monitorModel.getWeixinCorpId());
            config.setCorpSecret(monitorModel.getWeixinCorpSecret());
            config.setAgentId(monitorModel.getWeixinAgentId());
            wxCpService = new WxCpServiceImpl();
            wxCpService.setWxCpConfigStorage(config);

            final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleWithFixedDelay(this,3L, monitorModel.getTestSpeedSec(), TimeUnit.SECONDS);

            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(()->{
                sendWeixinMsg("我是微信告警小助手，我一直都在检查服务器的状态");
            },3L,60L * 60 * 24,TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.error("启动应用异常",e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.showAndWait();
        }
    }

    @Override
    public void run() {
        List<PortModel> portList = monitorModel.getPortList();
        for (PortModel item : portList) {
            LOGGER.info("正在检查端口:{}",item);
            int errorTimes = 0;
            for (int i = 0; i < monitorModel.getTestTimes(); i++) {
                processValue.set(processValue.get() >= 1 ? 0 : processValue.add(0.05).doubleValue());
                try {
                    Socket socket = new Socket();
                    socket.setSoTimeout(monitorModel.getTestTimes());
                    socket.connect(new InetSocketAddress(item.getIp(),item.getPort()));
                    socket.close();
                    break;
                } catch (IOException e) {
                    LOGGER.error("第{}次检查端口失败{}",i, item);
                    errorTimes ++;
                }
            }
            if (errorTimes == monitorModel.getTestTimes()){
                sendWeixinMsg(item.getErrorMsg());
            }
        }
    }

    private void sendWeixinMsg(String text){
        try {
            WxCpMessage message = WxCpMessage.TEXT().agentId(monitorModel.getWeixinAgentId()).toUser(monitorModel.getUserIds()).content(text).build();
            wxCpService.messageSend(message);
        } catch (Exception e) {
            LOGGER.error("微信发送告警失败",e);
        }
    }
}
