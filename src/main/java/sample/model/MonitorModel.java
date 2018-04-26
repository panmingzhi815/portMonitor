package sample.model;

import com.alibaba.fastjson.JSON;
import lombok.Data;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Data
public class MonitorModel {
    private String weixinCorpId;
    private String weixinCorpSecret;
    private Integer weixinAgentId;
    private String userIds;
    private Long testSpeedSec;
    private Integer testTimes;

    private List<PortModel> portList;

    public static MonitorModel load(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            Files.createFile(path);
            createTempConfig().write(filePath);
        }
        byte[] bytes = Files.readAllBytes(path);
        String toJSON = new String(bytes, Charset.forName("utf-8"));
        return JSON.parseObject(toJSON, MonitorModel.class);
    }

    private static MonitorModel createTempConfig(){
        MonitorModel monitorModel = new MonitorModel();
        monitorModel.setWeixinCorpId("setWeixinCorpId");
        monitorModel.setWeixinCorpSecret("setWeixinCorpSecret");
        monitorModel.setWeixinAgentId(1);
        monitorModel.setUserIds("setUserIds");
        monitorModel.setTestTimes(3);
        monitorModel.setTestSpeedSec(600L);

        PortModel portModel = new PortModel();
        portModel.setIp("127.0.0.1");
        portModel.setPort(1433);
        portModel.setErrorMsg("服务器 127.0.0.1 端口 1433 会影响数据库访问，请及时排查故障");
        monitorModel.setPortList(Collections.singletonList(portModel));
        return monitorModel;
    }

    private void write(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Writer writer = new OutputStreamWriter(Files.newOutputStream(path),Charset.forName("utf-8"));
            String toJSONString = JSON.toJSONString(this, true);
            writer.write(toJSONString);
            writer.flush();
            writer.close();
        }
    }
}
