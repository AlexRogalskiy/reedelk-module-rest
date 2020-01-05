package com.reedelk.rest.client.strategy;

import com.reedelk.rest.client.HttpClientResponseException;
import com.reedelk.rest.client.response.HttpResponseMessageMapper;
import com.reedelk.rest.commons.DataMarker;
import com.reedelk.rest.commons.IsSuccessfulStatus;
import com.reedelk.runtime.api.component.OnResult;
import com.reedelk.runtime.api.message.FlowContext;
import com.reedelk.runtime.api.message.Message;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

class StreamResponseConsumer extends AbstractAsyncResponseConsumer<Void> {

    private Exception exception;

    private OnResult callback;
    private FlowContext flowContext;
    private ByteBuffer byteBuffer;
    private BlockingQueue<byte[]> queue = new LinkedTransferQueue<>();

    StreamResponseConsumer(OnResult callback, FlowContext flowContext, int responseByteBufferSize) {
        this.callback = callback;
        this.flowContext = flowContext;
        this.byteBuffer = ByteBuffer.allocate(responseByteBufferSize);
    }

    @Override
    protected void onResponseReceived(HttpResponse response) {
        Flux<byte[]> bytesStream;

        // HEAD method does not have an entity: the onContentReceived will never be called.
        // Our message body bytes stream will be therefore empty.
        if (response.getEntity() == null) {

            bytesStream = Flux.empty();

        } else {
            // Map the response to message and create a flux
            bytesStream = Flux.create(sink -> {

                try {

                    byte[] take = queue.take();

                    while (take != DataMarker.END) {

                        if (take == DataMarker.ERROR) {

                            throw exception;

                        } else {

                            sink.next(take);

                        }

                        take = queue.take();

                    }

                    sink.complete();

                } catch (Exception e) {

                    sink.error(e);

                } finally {

                    queue = null;

                    exception = null;
                }
            });
        }


        if (IsSuccessfulStatus.status(response.getStatusLine().getStatusCode())) {

            Message message = HttpResponseMessageMapper.map(response, bytesStream);

            callback.onResult(message, flowContext);

        } else {

            callback.onError(new HttpClientResponseException(response, bytesStream), flowContext);

        }
    }

    @Override
    protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        try {

            decoder.read(byteBuffer);

            byteBuffer.flip();

            byte[] destination = new byte[byteBuffer.remaining()];

            byteBuffer.get(destination);

            byteBuffer.clear();

            queue.offer(destination);

            if (decoder.isCompleted()) {

                queue.offer(DataMarker.END);

            }

        } catch (Exception exception) {

            this.exception = exception;

            queue.offer(DataMarker.ERROR);

        }
    }

    @Override
    protected void onEntityEnclosed(HttpEntity entity, ContentType contentType) throws IOException {
    }

    @Override
    protected Void buildResult(HttpContext context) throws Exception {
        //  Nothing to do
        return null;
    }

    @Override
    protected void releaseResources() {
        callback = null;
        byteBuffer = null;
        flowContext = null;
    }
}
