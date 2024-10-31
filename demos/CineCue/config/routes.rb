Rails.application.routes.draw do

  # Define the root path route ("/")
  root to: 'movies#index'

  get 'books', to: 'movies#books'
  post 'book/:id', to: 'movies#book', as: 'book'
  post 'unbook/:id', to: 'movies#unbook', as: 'unbook'

  get 'signup', to: 'registrations#signup'
  post 'signup', to: 'registrations#signup_form'

  get 'login', to: 'registrations#login'
  post 'login', to: 'registrations#login_form'

  get 'pass', to: 'registrations#edit_pass'
  patch 'pass', to: 'registrations#edit_pass_form'

  patch 'lang', to: 'registrations#edit_lang_form'

  get 'logout', to: 'registrations#logout'

  get '/404', to: 'errors#not_found'
  get '/500', to: 'errors#internal_server_error'

  # Catch-all route for undefined paths (triggers 404 error)
  match '*path', to: 'errors#not_found', via: :all

  # Health check route
  get 'up' => 'rails/health#show', as: :rails_health_check

end
