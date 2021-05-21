package intensiveteam;
import javax.persistence.*;
import org.apache.commons.beanutils.BeanUtils;

public class Booked extends AbstractEvent {

    private Long id;

    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    @PreRemove
    public void onPreRemove(){
        BookCanceled bookCanceled = new BookCanceled();
        try{
            BeanUtils.copyProperties(this, bookCanceled);
            bookCanceled.publishAfterCommit();
        }catch (Exception ex){

        }

    }
}