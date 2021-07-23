package myhotel;

public enum PayStatus {
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELED("Canceled")
    ;

    private final String val;

    PayStatus(String val) {
        this.val = val;
    }
    @Override
    public String toString(){
        return val;
    }

}