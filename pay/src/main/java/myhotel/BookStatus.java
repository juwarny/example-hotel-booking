package myhotel;

public enum BookStatus {
    BOOKED("Booked"),
    REJECTED("Rejected"),
    CANCELED("Canceled")
    ;

    private final String val;

    BookStatus(String val) {
        this.val = val;
    }
    @Override
    public String toString(){
        return val;
    }
}
