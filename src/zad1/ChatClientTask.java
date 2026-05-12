/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */

package zad1;

import java.util.List;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<Void> {
    private ChatClient client;

    private ChatClientTask(ChatClient c, List<String> msgs, int wait) {
        super(() -> {
            c.login();
            if (wait > 0) Thread.sleep(wait);
            for (String m : msgs) {
                c.send(m);
                if (wait > 0) Thread.sleep(wait);
            }
            c.logout();
            if (wait > 0) Thread.sleep(wait);
            return null;
        });
        this.client = c;
    }

    public static ChatClientTask create(ChatClient c, List<String> msg, int wait){

        return new ChatClientTask(c, msg, wait);
    }



    public ChatClient getClient(){
        return client;
    }


}  
