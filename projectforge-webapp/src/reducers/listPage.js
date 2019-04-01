import {
    LIST_PAGE_DATA_UPDATE_BEGIN,
    LIST_PAGE_DATA_UPDATE_SUCCESS,
    LIST_PAGE_FILTER_RESET_BEGIN,
    LIST_PAGE_FILTER_RESET_SUCCESS,
    LIST_PAGE_FILTER_SET,
    LIST_PAGE_LOAD_BEGIN,
    LIST_PAGE_LOAD_FAILURE,
    LIST_PAGE_LOAD_SUCCESS,
} from '../actions';

const initialState = {
    loading: false,
    error: undefined,
    ui: {},
    data: {},
    category: '',
    sorting: undefined,
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case LIST_PAGE_LOAD_BEGIN:
            return {
                ...initialState,
                loading: true,
                category: payload.category,
            };
        case LIST_PAGE_LOAD_SUCCESS:
            return {
                ...state,
                loading: false,
                filter: payload.filter,
                ui: payload.ui,
                data: payload.data,
                sorting: {
                    // TODO: MOVE TO SERVER
                    column: payload.ui.layout[0].columns[0].id,
                    direction: 'ASC',
                },
            };
        case LIST_PAGE_LOAD_FAILURE:
            return {
                ...state,
                loading: false,
                error: payload.error,
            };
        case LIST_PAGE_FILTER_SET:
            return {
                ...state,
                filter: {
                    ...state.filter,
                    [payload.id]: payload.newValue,
                },
            };
        case LIST_PAGE_FILTER_RESET_BEGIN:
            return {
                ...state,
                loading: true,
                error: undefined,
            };
        case LIST_PAGE_FILTER_RESET_SUCCESS:
            return {
                ...state,
                loading: false,
                filter: payload.filter,
            };
        case LIST_PAGE_DATA_UPDATE_BEGIN:
            return {
                ...state,
                loading: true,
                error: undefined,
            };
        case LIST_PAGE_DATA_UPDATE_SUCCESS:
            return {
                ...state,
                loading: false,
                data: payload.data,
            };
        default:
            return state;
    }
};

export default reducer;
