package myhotel.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    private Date startDate;
    private Date endDate;
    private Long guestId;
    private Long hostId;
    private Long roomId;
    private BookStatus status;
    private Integer price;

}
