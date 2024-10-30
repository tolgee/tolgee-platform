require 'json'
require 'faraday'

class ApplicationController < ActionController::Base

  before_action :set_current_user, :tolgit

  def set_current_user
    @user = User.find_by(id: session[:user_id])
  end

  def tolgit

    if @user
      cookies[:lang] = @user.lang
    end
    cookies[:lang] ||= "en"

    @translations = JSON.parse(Faraday.get('https://cdn.tolg.ee/93411aa4fca9f5c1c79802feb5355105/'+cookies[:lang]+'.json').body)

  end

end
