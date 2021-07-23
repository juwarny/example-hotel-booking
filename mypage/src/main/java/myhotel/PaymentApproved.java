package myhotel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentApproved extends AbstractEvent {

    private Long id;
    private Long bookId;
    private Long roomId;
    private Integer price;
    private Long hostId;
    private Long guestId;
    private Date startDate;
    private Date endDate;
    private PayStatus status;

}