import {applyMiddleware, createStore} from 'redux';
import {composeWithDevTools} from 'redux-devtools-extension';
import thunk from 'redux-thunk';

import {reducer} from '../reducers';
import {analyticsMiddleware} from "../analytics/AnalyticsMiddleware";

export default function configureStore() {

  const store = createStore(
    reducer,
    composeWithDevTools(
      applyMiddleware(analyticsMiddleware, thunk)
    ),
  );

  if (module.hot) {
    module.hot.accept('../reducers', () =>
      store.replaceReducer(reducer)
    );
  }

  return store;
}
