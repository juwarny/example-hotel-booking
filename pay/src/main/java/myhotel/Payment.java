package myhotel;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long bookId;
    private Long roomId;
    private Integer price;
    private Long hostId;
    private Long guestId;
    private Date startDate;
    private Date endDate;
    private PayStatus status;

    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();


    }

    @PostUpdate
    public void onPostUpdate(){
        PaymentCanceled paymentCanceled = new PaymentCanceled();
        BeanUtils.copyProperties(this, paymentCanceled);
        paymentCanceled.publishAfterCommit();


    }

}
