package com.mikwee.timebrowser;

public class PassthroughStreamLoader /*implements StreamModelLoader<InputStream> */ { /*
    @Override
    public DataFetcher<InputStream> getResourceFetcher(final InputStream model, int width, int height) {
        return new DataFetcher<InputStream>() {
            @Override
            public InputStream loadData(Priority priority) throws Exception {
                return model;
            }

            @Override
            public void cleanup() {
                try {
                    model.close();
                } catch (IOException e) {
                    Log.w("PassthroughDataFetcher", "Cannot clean up after stream", e);
                }
            }

            @Override
            public String getId() {
                return String.valueOf(System.currentTimeMillis()); // There's no way to have a meaningful value here,
                // which means that caching of straight-loaded InputStreams is not possible.
            }

            @Override
            public void cancel() {
                // do nothing
            }
        };
    }

    public static class Factory implements ModelLoaderFactory<InputStream, InputStream> {
        @Override
        public ModelLoader<InputStream, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new PassthroughStreamLoader();
        }

        @Override
        public void teardown() {
            // nothing to do
        }
    }

    */
}
