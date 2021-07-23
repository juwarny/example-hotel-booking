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
public class BookCanceled extends AbstractEvent {

    private Long id;
    private Date startDate;
    private Date endDate;
    private Long guestId;
    private Long hostId;
    private Long roomId;
    private BookStatus status;
    private Integer price;
}