class ApplicationController < ActionController::Base

  before_action :set_current_user, :tolgit

  def set_current_user
    @user = User.find_by(id: session[:user_id])
  end

  def tolgit

    api_key = "<set tolgee api key here>"
    project_id = "<set tolgee projectid here>"

    url = "https://app.tolgee.io/v2/projects/"+project_id+"/translations/en,es,fr,ur,ar,tr,hi"

    response = Faraday.get(url) do |req|
      req.headers['Accept'] = 'application/json'
      req.headers['X-API-Key'] = api_key
    end

    if @user
      cookies[:lang] = @user.lang
    end
    cookies[:lang] ||= "en"

    @translations = JSON.parse(response.body)[cookies[:lang]]

  end

end
