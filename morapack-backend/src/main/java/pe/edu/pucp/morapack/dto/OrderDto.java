package pe.edu.pucp.morapack.dto;

public class OrderDto {
    private Long id;
    private Integer packageCount;
    private String airportDestinationId;
    private Integer priority;
    private String clientId;
    private String status;
    private Integer day;
    private Integer hour;
    private Integer minute;

    public OrderDto() {}

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getPackageCount() { return packageCount; }
    public void setPackageCount(Integer packageCount) { this.packageCount = packageCount; }

    public String getAirportDestinationId() { return airportDestinationId; }
    public void setAirportDestinationId(String airportDestinationId) { this.airportDestinationId = airportDestinationId; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }

    public Integer getHour() { return hour; }
    public void setHour(Integer hour) { this.hour = hour; }

    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }
}
