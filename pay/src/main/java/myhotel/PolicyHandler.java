package myhotel;

import myhotel.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyHandler{
    @Autowired PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookCanceled_Canceled(@Payload BookCanceled bookCanceled){

        if(!bookCanceled.validate()) return;

        System.out.println("\n\n##### listener Canceled : " + bookCanceled.toJson() + "\n\n");

        if(bookCanceled.isMe()){
            List<Payment> payList = paymentRepository.findByBookId(bookCanceled.getId());
            if ((payList != null) && !payList.isEmpty()){
                paymentRepository.deleteAll(payList);
            }
        }

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
