package sample.model;

import lombok.Data;

@Data
public class PortModel {
    private String ip;
    private Integer port;
    private String errorMsg;
}
