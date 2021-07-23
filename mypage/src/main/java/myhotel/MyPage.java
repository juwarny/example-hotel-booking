package myhotel;

import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="MyPage_table")
public class MyPage {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Long bookId;
        private Long hostId;
        private Integer price;
        private Date startDate;
        private Date endDate;
        private Long roomId;
        private BookStatus bookStatus;
        private PayStatus payStatus;
        private Long payId;
        private Long guestId;

}
